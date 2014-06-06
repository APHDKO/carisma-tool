/*******************************************************************************
 * Copyright (c) 2011 Software Engineering Institute, TU Dortmund.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    {SecSE group} - initial API and implementation and/or initial documentation
 *******************************************************************************/
package carisma.check.staticcheck.evolution.securelinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.uml2.uml.Artifact;
import org.eclipse.uml2.uml.CommunicationPath;
import org.eclipse.uml2.uml.Dependency;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Node;
import org.eclipse.uml2.uml.Stereotype;

import carisma.check.staticcheck.securelinks.SecureLinksCheck;
import carisma.check.staticcheck.securelinks.SecureLinksHelper;
import carisma.check.staticcheck.securelinks.utils.AnalysisMessage;
import carisma.check.staticcheck.securelinks.utils.OutputTarget;
import carisma.core.analysis.AnalysisHost;
import carisma.core.analysis.RegisterNotInUseException;
import carisma.core.analysis.result.AnalysisResultMessage;
import carisma.core.analysis.result.StatusType;
import carisma.core.checks.CarismaCheck;
import carisma.core.checks.CheckParameter;
import carisma.core.logging.LogLevel;
import carisma.core.logging.Logger;
import carisma.evolution.AddElement;
import carisma.evolution.DelElement;
import carisma.evolution.Delta;
import carisma.evolution.DeltaElement;
import carisma.evolution.DeltaList;
import carisma.evolution.SubstElement;
import carisma.evolution.uml2.ModifierMap;
import carisma.evolution.uml2.UMLModifier;
import carisma.modeltype.uml2.StereotypeApplication;
import carisma.modeltype.uml2.TaggedValue;
import carisma.modeltype.uml2.UMLDeploymentHelper;
import carisma.modeltype.uml2.UMLHelper;
import carisma.modeltype.uml2.exceptions.ModelElementNotFoundException;
import carisma.profile.umlsec.UMLsec;
import carisma.profile.umlsec.UMLsecUtil;


public class SecureLinksEvolutionCheck implements CarismaCheck {

	public static final String DELTAS_REGISTER_KEY = "carisma.data.evolution.deltas";
	public static final String MODIFIERS_REGISTRY_KEY = "carisma.data.evolution.modifiers";

	private AnalysisHost host;
	
	private Model model = null;
	
	private DeltaList deltaList = null;
	private ModifierMap deltaModifiers = null;
	
	private UMLModifier deltaModifier = null;
	
	private SecureLinksCheck staticSecureLinks = null;
	
	private List<DeltaElement> processedDeltaElements = null;
	private List<AnalysisMessage> errorMessages = null;
	
	/**
	 * Constant name for the Tag 'adversary'.
	 */
	private static final String ADVERSARY = "adversary";
	
	
	@Override
	public boolean perform(final Map<String, CheckParameter> parameters, final AnalysisHost newHost) {
		host = newHost;
		
		Resource currentModel = host.getAnalyzedModel();
		if (currentModel.getContents().isEmpty()) {
			host.addResultMessage(new AnalysisResultMessage(StatusType.WARNING, "Empty model"));
			return false;
		}
		if (!(currentModel.getContents().get(0) instanceof Model)) {
			host.addResultMessage(new AnalysisResultMessage(StatusType.WARNING, "Content is not a model!"));
			return false;
		}
		model = (Model) currentModel.getContents().get(0);
		
		try {
			deltaList = (DeltaList) host.getFromRegister(DELTAS_REGISTER_KEY);
			deltaModifiers = (ModifierMap) host.getFromRegister(MODIFIERS_REGISTRY_KEY);
		} catch (RegisterNotInUseException e) {
			Logger.log(LogLevel.ERROR, e.getMessage(), e);
			return false;
		}
		if (deltaList == null || deltaModifiers == null) {
			return false;
		}
		if (deltaList.isEmpty()) {
			host.addResultMessage(new AnalysisResultMessage(StatusType.ERROR, "No deltaList left to analyze."));
			return true;
		}
		int beforeMaxChanges = deltaList.getHighestChangeCountNow();
		boolean isSuccessful = checkDeltas();
		if (isSuccessful) {
			host.addResultMessage(
					new AnalysisResultMessage(
							StatusType.INFO,
							"A successful maximum Delta (using " + deltaList.getHighestChangeCountNow() + " changes) exists."));
		} else {
			host.addResultMessage(new AnalysisResultMessage(StatusType.ERROR, "No successful maximum Delta (" + beforeMaxChanges + " changes) found."));
			if (deltaList.getHighestChangeCountNow() == 0) {
				host.addResultMessage(new AnalysisResultMessage(StatusType.ERROR, "All Deltas violate <<secure links>>."));
			} else {
				host.addResultMessage(
						new AnalysisResultMessage(
								StatusType.ERROR,
								"Maximum successful Delta has " + deltaList.getHighestChangeCountNow() + " changes."));
			}
		}
		return isSuccessful;
	}

		private void init(final Delta d) {
			deltaModifier = deltaModifiers.get(d);
			if (processedDeltaElements == null) {
				processedDeltaElements = new ArrayList<DeltaElement>();
			}
			processedDeltaElements.clear();
			if (errorMessages == null) {
				errorMessages = new ArrayList<AnalysisMessage>();
			}
			errorMessages.clear();
			staticSecureLinks = new SecureLinksCheck();
		}
		
		private boolean checkDeltas() {
			boolean hasMaxSuccessfulDelta = false;
			List<Delta> violatingEvolutions = new ArrayList<Delta>();
			for (Delta d : deltaList.getRemainingDeltas()) {
				boolean deltaSuccessful = true;
				checkDelta(d);
				for (AnalysisMessage errorMessage : errorMessages) {
					if (errorMessage.getType() == StatusType.ERROR) {
						violatingEvolutions.add(d);
						deltaSuccessful = false;
						break;
					}
				}
				for (AnalysisMessage errorMessage : errorMessages) {
					errorMessage.print(host);
				}
				if (deltaSuccessful 
						&& d.getNumberOfUsedChanges() == deltaList.getHighestChangeCountNow()) {
					hasMaxSuccessfulDelta = true;
				}
			}
			deltaList.removeAll(violatingEvolutions);
			return hasMaxSuccessfulDelta;
		}
		
		
		private void checkDelta(final Delta d) {
			init(d);
			processDelElements(d.getAllDeletions());
			processAddElements(d.getAllAdditions());
			processSubstElements(d.getAllSubstitutions());			
		}
		
		private void processDelElements(final List<DelElement> allDeletions) {
			for (DelElement delElement : allDeletions) {
				processDelElement(delElement);
				processedDeltaElements.add(delElement);
			}
		}
		
		private void processDelElement(final DeltaElement delElement) {
			EObject affectedElem = delElement.getTarget();
			if (affectedElem instanceof CommunicationPath) {
				processLinkDeletion(delElement);
			} else if (affectedElem instanceof StereotypeApplication) {
				processStereotypeDeletion(delElement);
			} else if (affectedElem instanceof TaggedValue) {
				processTaggedValueDeletion(delElement);
			} else {
				processOtherElementDeletion(delElement);
			}
		}
		
		private void processAddElements(final List<AddElement> allAdditions) {
			for (AddElement addElement : allAdditions) {
				processAddElement(addElement);
				processedDeltaElements.add(addElement);
				processContent(addElement);
			}
		}
		
		private void processContent(final AddElement addElement) {
			for (AddElement containedElement : addElement.getContent()) {
				processAddElement(containedElement);
				processedDeltaElements.add(containedElement);
				processContent(containedElement);
			}
		}
		
		private void processAddElement(final AddElement addElement) {
			EClass newElemClass = addElement.getMetaClass();
			AddElement addElemCopy = (AddElement) deltaModifier.get(addElement);
			if (newElemClass.getName().equalsIgnoreCase("Property")
					&& addElemCopy.getTarget() instanceof StereotypeApplication
					&& ((StereotypeApplication) addElemCopy.getTarget()).getAppliedStereotype().getName().equalsIgnoreCase("secure links")) {
				processAdversaryAddition(addElement);
			} else if (newElemClass.getName().equalsIgnoreCase("Stereotype") 
					&& addElemCopy.getTarget() instanceof Dependency) {
				processRequirementAddition(addElement);
			} else if (newElemClass.getName().equalsIgnoreCase("Stereotype") 
					&& addElemCopy.getTarget() instanceof CommunicationPath
					&& SecureLinksHelper.isSecureLinksLinktype((String) addElement.getValues().get("name"))) {
				processLinktypeAddition(addElement);
			} else if (newElemClass.getName().equalsIgnoreCase("Deployment")) {
				processDeploymentAddition(addElement);
			} else {
				processOtherElementAddition(addElement);
			}
		}
		
	// TODO: erste Version nur subst durch gleiche Elementklassen
		private void processSubstElements(final List<SubstElement> allSubstitutions) {
			for (SubstElement substElement : allSubstitutions) {
				processDelElement(substElement);
				for (AddElement component : substElement.getComponents()) {
					processAddElement(component);
				}
				processedDeltaElements.add(substElement);
			}
		}
		
		private void processLinkDeletion(final DeltaElement delElement) {
			CommunicationPath affectedLink = (CommunicationPath) delElement.getTarget();		
			if (!linkStillRequired(affectedLink)) {
				errorMessages.add(
						new AnalysisMessage(
								StatusType.INFO, OutputTarget.REPORT, 
								Messages.linkDeletionAllowed(delElement)));
			} else {
				if (deletedLinkIsReplaced(affectedLink)) {
					CommunicationPath postLink = getLinkReplacement(affectedLink);
					if (!staticSecureLinks.compliesWithRequirements(postLink).isEmpty()) {
						errorMessages.add(
								new AnalysisMessage(
										StatusType.ERROR, OutputTarget.BOTH, 
										Messages.linkDeletionNotAllowed(delElement)));
					} else {
						errorMessages.add(
								new AnalysisMessage(
										StatusType.INFO, OutputTarget.REPORT, 
										Messages.linkDeletionAllowed(delElement)));
					}
				} else {
					errorMessages.add(
							new AnalysisMessage(
									StatusType.ERROR, OutputTarget.BOTH, 
									Messages.linkDeletionNotAllowed(delElement)));
				}
			}
		}
		
		private void processLinktypeDeletion(final DeltaElement delElement) {
			StereotypeApplication stapp = (StereotypeApplication) delElement.getTarget();
			CommunicationPath affectedLink = (CommunicationPath) stapp.getExtendedElement();
			if (!linkStillRequired(affectedLink)) {
				errorMessages.add(
						new AnalysisMessage(
								StatusType.INFO, OutputTarget.REPORT, 
								Messages.linktypeDeletionAllowed(delElement)));
			} else {
				if (linkGetsNewLinktype(affectedLink)) {
					CommunicationPath postLink = getLinkReplacement(affectedLink);
					StereotypeApplication postLinktype = staticSecureLinks.getLinktype(postLink);
					if (!(staticSecureLinks.compliesWithRequirements(postLink).isEmpty())) {
						errorMessages.add(
								new AnalysisMessage(
										StatusType.ERROR, OutputTarget.BOTH, 
										Messages.linktypeReplacementWrong(delElement, postLink, postLinktype.getAppliedStereotype())));
					} else {
						errorMessages.add(
								new AnalysisMessage(
										StatusType.INFO, OutputTarget.REPORT, 
										Messages.linktypeDeletionAllowed(delElement)));
					}				
				} else {
					errorMessages.add(
							new AnalysisMessage(
									StatusType.ERROR, OutputTarget.BOTH, 
									Messages.linktypeDeletionNotAllowed(delElement)));
				}
			}
		}
		
		
		private void processStereotypeDeletion(final DeltaElement delElem) {
			StereotypeApplication deletedApplication =
				(StereotypeApplication) delElem.getTarget();
			Stereotype appliedStereo = deletedApplication.getAppliedStereotype();
			if (SecureLinksHelper.isSecureLinksLinktype(appliedStereo)) {
				processLinktypeDeletion(delElem);
			} else {
				if (appliedStereo.getName().equalsIgnoreCase("secure links")) {
					Element extElem = deletedApplication.getExtendedElement();
					Element extElemCopy = (Element) deltaModifier.getMapping().get(extElem);
					if (extElemCopy == null) {
						errorMessages.add(
								new AnalysisMessage(
										StatusType.INFO, OutputTarget.REPORT, 
										Messages.otherDeletion(delElem)));
					} else {
						StereotypeApplication secureLinksAppCopy = UMLsecUtil.getStereotypeApplication(extElemCopy, UMLsec.SECURE_LINKS);
						if (secureLinksAppCopy == null) {
							errorMessages.add(
									new AnalysisMessage(
											StatusType.WARNING, OutputTarget.BOTH, 
											Messages.secureLinksDeletion(delElem)));
						} else {
							List<String> tagValues = UMLsecUtil.getStringValues(ADVERSARY, UMLsec.SECURE_LINKS, extElem);
							List<String> tagValuesCopy = UMLsecUtil.getStringValues(ADVERSARY, UMLsec.SECURE_LINKS, extElemCopy);
							if (tagValues.size() == tagValuesCopy.size() && tagValues.containsAll(tagValuesCopy)) {
								errorMessages.add(
										new AnalysisMessage(
												StatusType.INFO, OutputTarget.REPORT, 
												Messages.otherDeletion(delElem)));
							} else {
								errorMessages.add(
										new AnalysisMessage(
												StatusType.ERROR, OutputTarget.BOTH, 
												Messages.adversaryTagAdded()));
							}
						}
					}
				} else {
					errorMessages.add(
							new AnalysisMessage(
									StatusType.INFO, OutputTarget.REPORT, 
									Messages.otherDeletion(delElem)));
				}
			}
		}
		
		private void processTaggedValueDeletion(final DeltaElement delElem) {
			TaggedValue tag = (TaggedValue) delElem.getTarget();
			if (tag.getName().equalsIgnoreCase(ADVERSARY)) {
				String tagValue = (String) tag.getValue();
				if (tagValue.equalsIgnoreCase("default")) {
					errorMessages.add(
							new AnalysisMessage(
									StatusType.INFO, OutputTarget.REPORT, 
									Messages.otherDeletion(delElem)));
				} else {
					Element extElemCopy = (Element) deltaModifier.getMapping().get(tag.getCorrespondingApplication().getExtendedElement());
					if (extElemCopy == null) {
						errorMessages.add(
								new AnalysisMessage(
										StatusType.INFO, OutputTarget.REPORT, 
										Messages.otherDeletion(delElem)));
					} else {
						List<String> tagValueCopy = UMLsecUtil.getStringValues(ADVERSARY, UMLsec.SECURE_LINKS, extElemCopy);
						if (tagValueCopy.contains(tagValue)) {
							errorMessages.add(
									new AnalysisMessage(
											StatusType.INFO, OutputTarget.REPORT, 
											Messages.otherDeletion(delElem)));
						} else {
							errorMessages.add(
									new AnalysisMessage(
											StatusType.ERROR, OutputTarget.BOTH, 
											Messages.adversaryTagDeleted(delElem)));
						}
					}
				}
			}
		}
		
		private void processOtherElementDeletion(final DeltaElement delElem) {
			errorMessages.add(
					new AnalysisMessage(
							StatusType.INFO, OutputTarget.REPORT, 
							Messages.otherDeletion(delElem)));
		}
		
		private void processAdversaryAddition(final AddElement addElement) {
			String adversaryValue = (String) addElement.getValues().get("value");
			if (!adversaryValue.equalsIgnoreCase("default")) {
				errorMessages.add(
						new AnalysisMessage(
								StatusType.ERROR, OutputTarget.BOTH, 
								Messages.adversaryTagAdded()));
			}
		}

		private void processRequirementAddition(final AddElement addElement) {
			AddElement addElementCopy = (AddElement) deltaModifier.get(addElement);
			String requirementName = (String) addElement.getValues().get("name");
			Dependency depCopy = (Dependency) addElementCopy.getTarget();
			if (depCopy != null) {
				Stereotype newStereo = depCopy.getAppliedStereotype(requirementName);
				for (CommunicationPath commPath : UMLDeploymentHelper.getCommunicationPaths(depCopy)) {
					if (!staticSecureLinks.compliesWithRequirement(commPath, newStereo).isEmpty()) {
						StereotypeApplication linktype = staticSecureLinks.getLinktype(commPath);
						errorMessages.add(
								new AnalysisMessage(
										StatusType.ERROR, OutputTarget.BOTH, 
										Messages.newRequirementNotMet(addElement, commPath, linktype.getAppliedStereotype())));
					}
				}
			}
		}
		
		private void processLinktypeAddition(final AddElement addElement) {
			AddElement addElementCopy = (AddElement) deltaModifier.get(addElement);
			CommunicationPath parentLinkCopy = (CommunicationPath) addElementCopy.getTarget();
			if (parentLinkCopy != null) {
				for (Dependency dep : UMLDeploymentHelper.getAllDependencies(parentLinkCopy)) {
					if (!staticSecureLinks.compliesWithRequirements(parentLinkCopy, dep).isEmpty()) {
						errorMessages.add(
								new AnalysisMessage(
										StatusType.ERROR, OutputTarget.BOTH, 
										Messages.newLinktypeNotAppropriate(addElement, dep)));
					} else {
						processOtherElementAddition(addElement);
					}
				}
			}
		}
		
		private void processDeploymentAddition(final AddElement addElement) {
			String locationName = (String) addElement.getValues().get("location");
			String artifactName = (String) addElement.getValues().get("deployedArtifact");
			Model modelCopy = (Model) deltaModifier.getMapping().get(model);
			try {
				Node locationCopy = (Node) UMLHelper.getElementByName(modelCopy, locationName);
				Artifact deployedArtifactCopy = (Artifact) UMLHelper.getElementByName(modelCopy, artifactName);

				if (locationCopy != null && deployedArtifactCopy != null) {
					for (Dependency depCopy : SecureLinksHelper.getAllDependenciesWithRequirements(deployedArtifactCopy)) {
						Set<Artifact> allOtherArtifacts = UMLDeploymentHelper.getAllArtifacts(depCopy);			
						allOtherArtifacts.remove(deployedArtifactCopy);
						for (Node nodeCopy : UMLDeploymentHelper.getDeploymentLocations(allOtherArtifacts)) {
							CommunicationPath commPath = UMLDeploymentHelper.getCommunicationPath(locationCopy, nodeCopy);
							if (commPath == null) {
								errorMessages.add(
										new AnalysisMessage(
												StatusType.ERROR, OutputTarget.BOTH, 
												Messages.newDeploymentNoLink(addElement, locationCopy, nodeCopy, deployedArtifactCopy, depCopy)));
							} else {
								StereotypeApplication linktype = staticSecureLinks.getLinktype(commPath);
								if (linktype == null) {
									errorMessages.add(
											new AnalysisMessage(
													StatusType.ERROR, OutputTarget.BOTH, 
													Messages.newLinkNoLinktype(addElement, commPath, depCopy)));								
								} else if (!staticSecureLinks.compliesWithRequirements(commPath, depCopy).isEmpty()) {
									errorMessages.add(
											new AnalysisMessage(
													StatusType.ERROR, OutputTarget.BOTH, 
													Messages.newDeploymentLinkNotAppropriate(addElement, locationCopy, depCopy, commPath, linktype.getAppliedStereotype())));
								}
							}
						}
					}
				}
			}
			catch (ModelElementNotFoundException e) {
	            Logger.log(LogLevel.ERROR, e.getMessage(), e);
			}
		}
		
		private void processOtherElementAddition(final AddElement addElement) {
			errorMessages.add(
					new AnalysisMessage(
							StatusType.INFO, OutputTarget.REPORT, 
							Messages.otherElementAddition(addElement)));
		}		
		
		/**
		 * Checks if, after applying the changes to the model,
		 * the link is still required (i.e. requirements are present).
		 * @param aLink - the link to check
		 * @return - true if usageDependency requirements still exist
		 */
		private boolean linkStillRequired(final CommunicationPath aLink) {
			List<Node> oldNodes = UMLDeploymentHelper.getNodes(aLink);
			if (oldNodes.size() != 2) {
				return false;
			}
			List<Node> newNodes = new ArrayList<Node>();
			Node newNode1 = (Node) deltaModifier.getMapping().get(oldNodes.get(0));
			Node newNode2 = (Node) deltaModifier.getMapping().get(oldNodes.get(1));
			if (newNode1 == null || newNode2 == null) {
				return false;
			}
			newNodes.add(newNode1);
			newNodes.add(newNode2);
			if (staticSecureLinks.linkIsNeeded(newNodes)) {
				return true;
			}
			return false;
		}
		
		private boolean deletedLinkIsReplaced(final CommunicationPath theLink) {
			List<Node> oldNodes = UMLDeploymentHelper.getNodes(theLink);
			if (oldNodes.size() != 2) {
				return false;
			}
			Node newNode1 = (Node) deltaModifier.getMapping().get(oldNodes.get(0));
			Node newNode2 = (Node) deltaModifier.getMapping().get(oldNodes.get(1));
			if (newNode1 == null || newNode2 == null) {
				return false;
			}
			CommunicationPath newLink = UMLDeploymentHelper.getCommunicationPath(newNode1, newNode2);
			if (newLink == null) {
				return false;
			}
			return true;
		}
		
		private CommunicationPath getLinkReplacement(final CommunicationPath preLink) {
			if (preLink == null) {
				return null;
			}
			List<Node> oldNodes = UMLDeploymentHelper.getNodes(preLink);
			if (oldNodes.size() != 2) {
				return null;
			}
			Node newNode1 = (Node) deltaModifier.getMapping().get(oldNodes.get(0));
			Node newNode2 = (Node) deltaModifier.getMapping().get(oldNodes.get(1));
			if (newNode1 == null || newNode2 == null) {
				return null;
			}
			return UMLDeploymentHelper.getCommunicationPath(newNode1, newNode2);
		}
		
		private boolean linkGetsNewLinktype(final CommunicationPath theLink) {
			CommunicationPath linkCopy =
				(CommunicationPath) deltaModifier.getMapping().get(theLink);
			if (linkCopy == null) {
				return false;
			}
			StereotypeApplication postLinktype = staticSecureLinks.getLinktype(linkCopy);
			if (postLinktype != null) {
				return true;
			}
			return false;
		}
		
}
