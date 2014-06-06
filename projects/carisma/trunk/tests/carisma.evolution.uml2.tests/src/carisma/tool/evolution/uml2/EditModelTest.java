package carisma.tool.evolution.uml2;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueAction;
import org.junit.Test;


import org.eclipse.uml2.uml.Package;

import carisma.core.logging.LogLevel;
import carisma.core.logging.Logger;
import carisma.evolution.Delta;
import carisma.evolution.DeltaElement;
import carisma.evolution.EditElement;
import carisma.evolution.uml2.UMLModifier;
import carisma.modeltype.uml2.UML2ModelLoader;
import carisma.modeltype.uml2.UMLHelper;
import carisma.modeltype.uml2.exceptions.ModelElementNotFoundException;

/**
 * this JUnit test contains tests according to the stereotype <<edit>> for the UMLModifier.
 * @author Klaus Rudack
 *
 */
public class EditModelTest {

    /**
     * a UML2ModelLoader.
     */
    private UML2ModelLoader ml = null;
    
    /**
     * the model-resource.
     */
    private Resource modelres = null;
    
    /**
     * Name of an Element.
     */
    private static final String NEW_NAME = "NewName";
    
    /**
     * Error Message.
     */
    private static final String ERROR_NEW_NAME_NOT_FOUND = "Element '" + NEW_NAME + "' of type 'OpaqueAction' not found in ";  /**
    
     * Error Message.
     */
    private static final String ERROR_TARGET1_NOT_FOUND = "Element 'Target1' of type 'OpaqueAction' not found in ";  
    
    /**
     * loads the given model.
     * @param testmodelname - the model to load
     */
    private void loadModel(final String testmodelname) {
        String testmodeldir = "resources/models/modifier";
        File testmodelfile = new File(testmodeldir + File.separator + testmodelname);
        assertTrue(testmodelfile.exists());
        if (ml == null) {
            ml = new UML2ModelLoader();
        }
        try {
            modelres = ml.load(testmodelfile);
        } catch (IOException e) {
            Logger.log(LogLevel.ERROR, "Couldn't load model.", e);
            fail("Couldn't load model");
        }
    }
    
    
    /**
     * this test generates editElements and tests if a model will be edited correctly.
     */
    @Test
    public final void testEditsForModel() {
        loadModel("testCreateEditsForModel.uml");
        String controlFlow = "ControlFlow2";
        try {
            Model theOldModel = (Model) modelres.getContents().get(0);
            UMLModifier um;
            Model theNewModel;
            
            ControlFlow oldControlFlow2 = 
                    UMLHelper.getElementOfNameAndType(theOldModel, controlFlow, ControlFlow.class);
            assertNotNull(oldControlFlow2);
            EditElement editControlFlow2 = new EditElement(oldControlFlow2);
            editControlFlow2.addKeyValuePair("target", "Target2");
            editControlFlow2.addKeyValuePair("visibility", "private");
            List<DeltaElement> deltaContent = new ArrayList<DeltaElement>();
            deltaContent.add(editControlFlow2);
            OpaqueAction target1InOldModel = 
                    UMLHelper.getElementOfNameAndType(theOldModel, "Target1", OpaqueAction.class);
            EditElement editTarget1 = new EditElement(target1InOldModel);
            editTarget1.addKeyValuePair("name", NEW_NAME);
            deltaContent.add(editTarget1);
            Delta d = new Delta(deltaContent);
            um = new UMLModifier(modelres, d);
            theNewModel = um.getModifiedModel();
            ControlFlow newControlFlow2 =
                    UMLHelper.getElementOfNameAndType(theNewModel, "ControlFlow2", ControlFlow.class);
            assertNotNull(newControlFlow2);
            assertNotSame(oldControlFlow2, newControlFlow2);
            assertNotSame(oldControlFlow2.getVisibility(), newControlFlow2.getVisibility());
            if (wrappGetElementOfNameAndType(theOldModel, NEW_NAME, OpaqueAction.class) != null) {
                fail(ERROR_NEW_NAME_NOT_FOUND + theOldModel.getName());
            }
            
            if (wrappGetElementOfNameAndType(theNewModel, "Target1", OpaqueAction.class) != null) {
              fail(ERROR_TARGET1_NOT_FOUND + theNewModel.getName());
           }
            try {
                if (UMLHelper.getElementOfNameAndType(theNewModel, NEW_NAME, OpaqueAction.class) == null) {
                    fail(ERROR_NEW_NAME_NOT_FOUND + theNewModel.getName());  
                }
            } catch (ModelElementNotFoundException e) {
                fail(ERROR_NEW_NAME_NOT_FOUND + theNewModel.getName());
            }
        } catch (ModelElementNotFoundException e) {
            Logger.log(LogLevel.ERROR, "", e);
            fail(e.getMessage());       
        }
        modelres.unload();
    }
    
    /** Little helper Method which catches the original Exception and returns null instead.
     * This method is only called on expected null return value. 
     * 
     * @param pkg the package to search
     * @param name the name of the element to look for
     * @param type the class of the element to look for
     * @param <T> generic return Type.
     * @return the found Object or null.
     */
    private <T extends NamedElement> T wrappGetElementOfNameAndType(final Package pkg, final String name, final Class<T> type) {
        try {
           return UMLHelper.getElementOfNameAndType(pkg, name, type);
        } catch (ModelElementNotFoundException e) {
            return null;
        }
    }
    
    
    /**
     * tests what happens with wrong edits.
     */
    @Test
    public final void testIncorrectEdits() {
        loadModel("testCreateEditsForModel.uml");
        String controlFlow = "ControlFlow2";
        try {
            Model theOldModel = (Model) modelres.getContents().get(0);
            UMLModifier um;
            Model theNewModel;
            ControlFlow target = (ControlFlow) UMLHelper.getElementByName(theOldModel, controlFlow);
            assertNotNull(target);
            assertEquals("public", target.getVisibility().getLiteral());
            ControlFlow newTarget = null;
            try {
                newTarget = UMLHelper.getElementOfNameAndType(theOldModel, "newCF", ControlFlow.class);
            } catch (ModelElementNotFoundException e) {
                assertNull(newTarget);              
            }
            assertNull(newTarget);
            EditElement editElement1 = new EditElement(target);
            editElement1.addKeyValuePair("name", "newCF");
            editElement1.addKeyValuePair("visibility", "private");
            List<DeltaElement> delList = new ArrayList<DeltaElement>();
            delList.add(editElement1);
            Delta d = new Delta(delList);
            um = new UMLModifier(modelres, d);
            theNewModel = um.getModifiedModel();
            target = null;
            try {
                target = (ControlFlow) UMLHelper.getElementByName(theNewModel, controlFlow);
            } catch (ModelElementNotFoundException e) {
                assertNull(target);             
            }
            assertNull(target);
            newTarget = (ControlFlow) UMLHelper.getElementByName(theNewModel, "newCF");
            assertEquals("private", newTarget.getVisibility().getLiteral());
            assertNotNull(newTarget);
        } catch (ModelElementNotFoundException e) {
            Logger.log(LogLevel.ERROR, "", e);
            fail(e.getMessage());       
        }
        
        modelres.unload();
    }
}
