/*******************************************************************************
 * Copyright (c) 2013-2015 LAAS-CNRS (www.laas.fr) 
 * 7 Colonel Roche 31077 Toulouse - France
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Thierry Monteil (Project co-founder) - Management and initial specification, 
 * 		conception and documentation.
 *     Mahdi Ben Alaya (Project co-founder) - Management and initial specification, 
 * 		conception, implementation, test and documentation.
 *     Christophe Chassot - Management and initial specification.
 *     Khalil Drira - Management and initial specification.
 *     Yassine Banouar - Initial specification, conception, implementation, test 
 * 		and documentation.
 *     Guillaume Garzone - Conception, implementation, test and documentation.
 *     Francois Aissaoui - Conception, implementation, test and documentation.
 ******************************************************************************/
package org.eclipse.om2m.core.controller;

import java.util.Date;

import javax.persistence.EntityManager;

import org.eclipse.om2m.commons.resource.ErrorInfo;
import org.eclipse.om2m.commons.resource.Refs;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.resource.Subscription;
import org.eclipse.om2m.commons.resource.SubscriptionType;
import org.eclipse.om2m.commons.resource.Subscriptions;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.commons.utils.DateConverter;
import org.eclipse.om2m.commons.utils.XmlMapper;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.dao.DAOFactory;
import org.eclipse.om2m.core.dao.DBAccess;

/**
 * Implements Create, Retrieve, Update, Delete and Execute methods to handle
 * generic REST request for {@link Subscription} resource.
 *
 * @author <ul>
 *         <li>Yassine Banouar < ybanouar@laas.fr > < yassine.banouar@gmail.com ></li>
 *         <li>Mahdi Ben Alaya < ben.alaya@laas.fr > < benalaya.mahdi@gmail.com ></li>       
 *         </ul>
 */

public class SubscriptionController extends Controller {

    /**
     * Creates {@link Subscription} resource.
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */

    public ResponseConfirm doCreate (RequestIndication requestIndication) {

        // expirationTime:                  (createReq O)  (response M*)
        // minimalTimeBetweenNotifications: (createReq O)  (response O)
        // delayTolerance:                  (createReq O)  (response O)
        // creationTime:                    (createReq NP) (response M)
        // lastModifiedTime:                (createReq NP) (response M)
        // filterCriteria:                  (createReq O)  (response O)
        // subscritpionType:                (createReq NP) (response M)
        // contact:                         (createReq M)  (response M)
        // Id:                              (createReq O)  (response M*)


        ResponseConfirm errorResponse = new ResponseConfirm();
//        Subscriptions subscriptions = DAOFactory.getSubscriptionsDAO().lazyFind(requestIndication.getTargetID());
        EntityManager em = DBAccess.createEntityManager();
        em.getTransaction().begin();
        String accessRightID = this.getAccessRightId(requestIndication.getTargetID(), em);
        
        // Check Parent Existence
        if (accessRightID == null) {
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_FOUND,requestIndication.getTargetID()+" does not exist")) ;
        }
        // Check AccessRight
        errorResponse = checkAccessRight(accessRightID, requestIndication.getRequestingEntity(), Constants.AR_CREATE);
        if (errorResponse != null) {
            return errorResponse;
        }
        // Check Resource Representation
        if (requestIndication.getRepresentation() == null) {
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Resource Representation is EMPTY")) ;
        }
        // Checks on attributes
        Subscription subscription = null ; 
        try{
        	subscription = (Subscription) XmlMapper.getInstance().xmlToObject(requestIndication.getRepresentation());
        } catch (ClassCastException e){
        	em.close();
        	LOGGER.debug("ClassCastException : Incorrect resource type in JAXB unmarshalling.",e);
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST, "Incorrect resource type"));
        }
        if (subscription == null){
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST, "Incorrect resource representation syntax")) ;
        }
        // Check ExpirationTime
        if (subscription.getExpirationTime() != null && !checkExpirationTime(subscription.getExpirationTime())) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Expiration Time CREATE is Out of Date")) ;
        }
        // Contact Attribute is mandatory
        if (subscription.getContact() == null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Link attribute is mandatory")) ;
        }
        // CreationTime Must be NP
        if (subscription.getCreationTime() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Creation Time CREATE is Not Permitted")) ;
        }
        // LastModifiedTime Must be NP
        if (subscription.getLastModifiedTime() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Last Modified Time CREATE is Not Permitted")) ;
        }
        // subscriptionType Must be NP
        if (subscription.getSubscriptionType() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"SubscriptionType CREATE is Not Permitted")) ;
        }
        // Check ContactURI conflict
        if (checkContactURIExistence(requestIndication.getTargetID(), subscription.getContact(),em)) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_CONFLICT,"Subscription ContactURI Conflict")) ;
        }
        // Storage
        // Check the ID conformity
        if (subscription.getId() != null && !subscription.getId().matches(Constants.ID_REGEXPR)) {
        	em.close();
        	return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"ID should match the following regexpr: " + Constants.ID_REGEXPR)) ;
        }
        // Check the ID uniqueness
        if (subscription.getId() != null && DAOFactory.getSubscriptionDAO().find(requestIndication.getTargetID()+"/"+subscription.getId(), em) != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_CONFLICT,"SubscriptionId Conflit")) ;
        }
        // Generates an ID if not set
        if (subscription.getId() == null || subscription.getId().isEmpty()) {
            subscription.setId(generateId("SUB_",""));
        }
        // Set URI
        subscription.setUri(requestIndication.getTargetID()+ "/" +subscription.getId());
        // Set Expiration Time if it's null
        if (subscription.getExpirationTime() == null) {
            //infinity expiration Time
            subscription.setExpirationTime(getNewExpirationTime(Constants.EXPIRATION_TIME));
        }
        // Set the subscription to asynchronous
        subscription.setSubscriptionType(SubscriptionType.ASYNCHRONOUS);

        //Set CreationTime
        subscription.setCreationTime(DateConverter.toXMLGregorianCalendar(new Date()).toString());
        subscription.setLastModifiedTime(DateConverter.toXMLGregorianCalendar(new Date()).toString());

        //Store subscription
        DAOFactory.getSubscriptionDAO().create(subscription, em);
        em.getTransaction().commit();
        em.close();
        // Response
        return new ResponseConfirm(StatusCode.STATUS_CREATED, subscription);
    }

    /**
     * Retrieves {@link Subscription} resource.
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */
    public ResponseConfirm doRetrieve (RequestIndication requestIndication) {

        // expirationTime:                  (response M*)
        // minimalTimeBetweenNotifications: (response O)
        // delayTolerance:                  (response O)
        // creationTime:                    (response M)
        // lastModifiedTime:                (response M)
        // filterCriteria:                  (response O)
        // subscritpionType:                (response M)
        // contact:                         (response M)
        // Id:                              (response M*)

        ResponseConfirm errorResponse = new ResponseConfirm();
        EntityManager em = DBAccess.createEntityManager();
        em.getTransaction().begin();
        Subscription subscription = DAOFactory.getSubscriptionDAO().find(requestIndication.getTargetID(), em);

        // Check if the resource exists in DataBase or Not
        if (subscription == null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_FOUND,requestIndication.getTargetID()+" does not exist in DataBase")) ;
        }
        String parentUri = subscription.getUri().split(Refs.SUBSCRIPTIONS_REF)[0];
        // Check AccessRight
        errorResponse = checkAccessRight(getAccessRightId(parentUri,em), requestIndication.getRequestingEntity(), Constants.AR_READ);
        if (errorResponse != null) {
        	em.close();
            return errorResponse;
        }
        em.close();
        // Response
        return new ResponseConfirm(StatusCode.STATUS_OK, subscription);

    }

    /**
     * Updates {@link Subscription} resource.
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */
    public ResponseConfirm doUpdate (RequestIndication requestIndication) {

        // expirationTime:                  (updateReq O)  (response M*)
        // minimalTimeBetweenNotifications: (updateReq O)  (response O)
        // delayTolerance:                  (updateReq O)  (response O)
        // creationTime:                    (updateReq NP) (response M)
        // lastModifiedTime:                (updateReq NP) (response M)
        // filterCriteria:                  (updateReq NP) (response O)
        // subscritpionType:                (updateReq NP) (response M)
        // contact:                         (updateReq NP) (response M)
        // Id:                              (updateReq NP) (response M*)

        ResponseConfirm errorResponse = new ResponseConfirm();
        EntityManager em = DBAccess.createEntityManager();
        em.getTransaction().begin();
        Subscription subscription = DAOFactory.getSubscriptionDAO().find(requestIndication.getTargetID(), em);

        // Check Existence
        if (subscription == null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_FOUND,requestIndication.getTargetID()+" does not exist in DataBase")) ;
        }
        String parentUri = subscription.getUri().split(Refs.SUBSCRIPTIONS_REF)[0];
        // Check AccessRight
        errorResponse = checkAccessRight(getAccessRightId(parentUri,em), requestIndication.getRequestingEntity(), Constants.AR_WRITE);
        if (errorResponse != null) {
        	em.close();
            return errorResponse;
        }
        // Check Resource Representation
        if (requestIndication.getRepresentation() == null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Resource Representation is EMPTY")) ;
        }
        // Check attributes
        Subscription subscriptionNew = null ; 
        try{
        	subscriptionNew = (Subscription) XmlMapper.getInstance().xmlToObject(requestIndication.getRepresentation());
        } catch (ClassCastException e){
        	em.close();
        	LOGGER.debug("ClassCastException : Incorrect resource type in JAXB unmarshalling.",e);
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST, "Incorrect resource type"));
        }
        if (subscriptionNew == null){
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST, "Incorrect resource representation syntax")) ;
        }

        // SubscriptionId UPDATE is NP
        if (subscriptionNew.getId() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"subscriptionId UPDATE is Not Permitted")) ;
        }
        // Contact Update is NP
        if (subscriptionNew.getContact() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Contact attribute UPDATE is Not Permitted")) ;
        }
        // SubscriptionType UPDATE is NP
        if (subscriptionNew.getSubscriptionType() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"subscriptionType UPDATE is Not Permitted")) ;
        }
        // FilterCriteria UPDATE is NP
        if (subscriptionNew.getFilterCriteria() != null) {
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"filterCriteria UPDATE is Not Permitted")) ;
        }
        // CreattionTime UPDATE is NP
        if (subscriptionNew.getCreationTime() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"CreationTime UPDATE is Not Permitted")) ;
        }
        // lastModifiedTime UPDATE is NP
        if (subscriptionNew.getLastModifiedTime() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"LatModifiedTime UPDATE is Not Permitted")) ;
        }
        // Check ExpirationTime
        if (subscriptionNew.getExpirationTime() != null && !checkExpirationTime(subscriptionNew.getExpirationTime())) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Expiration Time Update is Out of Date")) ;
        }

        // Storage
        // Set ExpirationTime
        if (subscriptionNew.getExpirationTime() != null) {
            subscription.setExpirationTime(subscriptionNew.getExpirationTime());
        }
        // Set MinimalTimeBetweenNotifications
        if (subscriptionNew.getMinimalTimeBetweenNotifications() != null) {
            subscription.setMinimalTimeBetweenNotifications(subscriptionNew.getMinimalTimeBetweenNotifications());
        }
        // Set DelayTolerance
        if (subscriptionNew.getDelayTolerance() != null) {
            subscription.setDelayTolerance(subscriptionNew.getDelayTolerance());
        }
        // Set LastModifiedTime
        subscription.setLastModifiedTime(DateConverter.toXMLGregorianCalendar(new Date()).toString());

        // Store container
        DAOFactory.getSubscriptionDAO().update(subscription, em);
        em.getTransaction().commit();
        em.close();
        // Response
        return new ResponseConfirm(StatusCode.STATUS_OK, subscription);

    }

    /**
     * Deletes {@link Subscription} resource.
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */
    public ResponseConfirm doDelete (RequestIndication requestIndication) {
    	EntityManager em = DBAccess.createEntityManager();
    	em.getTransaction().begin();
        Subscription subscription = DAOFactory.getSubscriptionDAO().find(requestIndication.getTargetID(), em);
        ResponseConfirm errorResponse = new ResponseConfirm();

        // Check Resource Existence
        if (subscription == null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_FOUND,requestIndication.getTargetID()+" does not exist")) ;
        }
        String parentUri = subscription.getUri().split(Refs.SUBSCRIPTIONS_REF)[0];
        // Check AccessRight
        errorResponse = checkAccessRight(getAccessRightId(parentUri,em), requestIndication.getRequestingEntity(), Constants.AR_DELETE);
        if (errorResponse != null) {
        	em.close();
            return errorResponse;
        }
        //Delete
        DAOFactory.getSubscriptionDAO().delete(subscription,em);
        em.getTransaction().commit();
        em.close();
        // Response
        return new ResponseConfirm(StatusCode.STATUS_OK);

    }

    /**
     * Executes {@link Subscription} resource.
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */
    public ResponseConfirm doExecute (RequestIndication requestIndication) {

        // Response
        return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED,requestIndication.getMethod()+" Method is not yet Implemented")) ;
    }

    /**
     * Checks Contact URI existence insure the uniqueness of the contact URI between the subscription resources in the same level
     * @param targetId
     * @param contact
     * @return true if contactURI exists otherwise false
     */
    public boolean checkContactURIExistence (String targetId, String contact, EntityManager em) {
        Subscriptions subscriptions = DAOFactory.getSubscriptionsDAO().find(targetId, em);

        for (int i=0; i<subscriptions.getSubscriptionCollection().getNamedReference().size(); i++) {
            Subscription subscription = DAOFactory.getSubscriptionDAO().find(targetId+"/"+subscriptions.getSubscriptionCollection().getNamedReference().get(i).getId(), em);

            if (subscription.getContact().equalsIgnoreCase(contact)) {
                return true;
            }
        }

        return false;
    }

}
