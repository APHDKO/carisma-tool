/**
 * Copyright (c) 2011 Software Engineering Institute, TU Dortmund.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     {SecSE group} - initial API and implementation and/or initial documentation
 */
package carisma.modeltype.owl2.model.owl.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

import carisma.modeltype.owl2.model.owl.ObjectExistsSelf;
import carisma.modeltype.owl2.model.owl.ObjectPropertyExpression;
import carisma.modeltype.owl2.model.owl.OwlPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Object Exists Self</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link carisma.modeltype.owl2.model.owl.impl.ObjectExistsSelfImpl#getObjectPropertyExpression <em>Object Property Expression</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ObjectExistsSelfImpl extends ClassExpressionImpl implements ObjectExistsSelf {
	/**
	 * The cached value of the '{@link #getObjectPropertyExpression() <em>Object Property Expression</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getObjectPropertyExpression()
	 * @generated
	 * @ordered
	 */
	protected ObjectPropertyExpression objectPropertyExpression;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ObjectExistsSelfImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OwlPackage.Literals.OBJECT_EXISTS_SELF;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ObjectPropertyExpression getObjectPropertyExpression() {
		if (objectPropertyExpression != null && objectPropertyExpression.eIsProxy()) {
			InternalEObject oldObjectPropertyExpression = (InternalEObject)objectPropertyExpression;
			objectPropertyExpression = (ObjectPropertyExpression)eResolveProxy(oldObjectPropertyExpression);
			if (objectPropertyExpression != oldObjectPropertyExpression) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, OwlPackage.OBJECT_EXISTS_SELF__OBJECT_PROPERTY_EXPRESSION, oldObjectPropertyExpression, objectPropertyExpression));
			}
		}
		return objectPropertyExpression;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ObjectPropertyExpression basicGetObjectPropertyExpression() {
		return objectPropertyExpression;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setObjectPropertyExpression(ObjectPropertyExpression newObjectPropertyExpression) {
		ObjectPropertyExpression oldObjectPropertyExpression = objectPropertyExpression;
		objectPropertyExpression = newObjectPropertyExpression;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OwlPackage.OBJECT_EXISTS_SELF__OBJECT_PROPERTY_EXPRESSION, oldObjectPropertyExpression, objectPropertyExpression));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OwlPackage.OBJECT_EXISTS_SELF__OBJECT_PROPERTY_EXPRESSION:
				if (resolve) return getObjectPropertyExpression();
				return basicGetObjectPropertyExpression();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OwlPackage.OBJECT_EXISTS_SELF__OBJECT_PROPERTY_EXPRESSION:
				setObjectPropertyExpression((ObjectPropertyExpression)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OwlPackage.OBJECT_EXISTS_SELF__OBJECT_PROPERTY_EXPRESSION:
				setObjectPropertyExpression((ObjectPropertyExpression)null);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OwlPackage.OBJECT_EXISTS_SELF__OBJECT_PROPERTY_EXPRESSION:
				return objectPropertyExpression != null;
		}
		return super.eIsSet(featureID);
	}

} //ObjectExistsSelfImpl
