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
import org.eclipse.om2m.commons.resource.SclBase;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.commons.utils.DateConverter;
import org.eclipse.om2m.commons.utils.XmlMapper;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.dao.DAOFactory;
import org.eclipse.om2m.core.dao.DBAccess;
import org.eclipse.om2m.core.notifier.Notifier;

/**
 * Implements Create, Retrieve, Update, Delete and Execute methods to handle
 * generic REST request for {@link SclBase} resource.
 *
 * @author <ul>
 *         <li>Yassine Banouar < ybanouar@laas.fr > < yassine.banouar@gmail.com ></li>
 *         <li>Mahdi Ben Alaya < ben.alaya@laas.fr > < benalaya.mahdi@gmail.com ></li>       
 *         </ul>
 */

public class SclBaseController extends Controller {

    /**
     * Creates {@link SclBase} resource. It is not allowed Through the API
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */
    public ResponseConfirm doCreate (RequestIndication requestIndication) {

        // sclsReference:           (createReq NA) (response M)
        // applicationsReference:   (createReq NA) (response M)
        // containersReference:     (createReq NA) (response M)
        // groupsReference:         (createReq NA) (response M)
        // accessRightsReference:   (createReq NA) (response M)
        // subscriptionsReference:  (createReq NA) (response M)
        // discoveryReference:      (createReq NA) (response M)
        // accessRightID:           (createReq NA) (response O)
        // searchStrings:           (createReq NA) (response M)
        // creationTime:            (createReq NA) (response M)
        // lastModifiedTime:        (createReq NA) (response M)
        // aPocHandling:            (createReq NA) (response O)

        // Response
        return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_METHOD_NOT_ALLOWED,requestIndication.getMethod()+" Method is not allowed"));
    }

    /**
     * Retrieves {@link SclBase} resource.
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */
    public ResponseConfirm doRetrieve (RequestIndication requestIndication) {

        // sclsReference:           (response M)
        // applicationsReference:   (response M)
        // containersReference:     (response M)
        // groupsReference:         (response M)
        // accessRightsReference:   (response M)
        // subscriptionsReference:  (response M)
        // discoveryReference:      (response M)
        // accessRightID:           (response O)
        // searchStrings:           (response M)
        // creationTime:            (response M)
        // lastModifiedTime:        (response M)
        // aPocHandling:            (response O)

        ResponseConfirm errorResponse = new ResponseConfirm();
        EntityManager em = DBAccess.createEntityManager();
        em.getTransaction().begin();
        SclBase sclBase = DAOFactory.getSclBaseDAO().find(requestIndication.getTargetID(), em);
        em.close();
        if (sclBase == null) {
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_FOUND,requestIndication.getTargetID()+ "does not exist")) ;
        }
        // Check AccessRight
        errorResponse = checkAccessRight(sclBase.getAccessRightID(), requestIndication.getRequestingEntity(), Constants.AR_READ);
        if (errorResponse != null) {
            return errorResponse;
        }

		sclBase.setAccessRightsReference(sclBase.getUri() + Refs.ACCESSRIGHTS_REF);
		sclBase.setApplicationsReference(sclBase.getUri() + Refs.APPLICATIONS_REF);
		sclBase.setContainersReference(sclBase.getUri() + Refs.CONTAINERS_REF);
		sclBase.setDiscoveryReference(sclBase.getUri() + "/discovery");
		sclBase.setGroupsReference(sclBase.getUri() + Refs.GROUPS_REF);
		sclBase.setSclsReference(sclBase.getUri() + Refs.SCLS_REF);
		sclBase.setSubscriptionsReference(sclBase.getUri() + Refs.SUBSCRIPTIONS_REF);
		
        // Response
        return new ResponseConfirm(StatusCode.STATUS_OK, sclBase);
    }

    /**
     * Updates {@link SclBase} resource.
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */
    public ResponseConfirm doUpdate (RequestIndication requestIndication) {

        // sclsReference:           (updateReq NP) (response M)
        // applicationsReference:   (updateReq NP) (response M)
        // containersReference:     (updateReq NP) (response M)
        // groupsReference:         (updateReq NP) (response M)
        // accessRightsReference:   (updateReq NP) (response M)
        // subscriptionsReference:  (updateReq NP) (response M)
        // discoveryReference:      (updateReq NP) (response M)
        // accessRightID:           (updateReq O)  (response O)
        // searchStrings:           (updateReq O)  (response M)
        // creationTime:            (updateReq NP) (response M)
        // lastModifiedTime:        (updateReq NP) (response M)
        // aPocHandling:            (updateReq O)  (response O)

        ResponseConfirm errorResponse = new ResponseConfirm();
        EntityManager em = DBAccess.createEntityManager();
        em.getTransaction().begin();
        SclBase sclBase = DAOFactory.getSclBaseDAO().find(requestIndication.getTargetID(), em);

        // Check resource Existence
        if (sclBase == null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_FOUND,requestIndication.getTargetID()+ "does not exist")) ;
        }
        // Check AccessRight
        errorResponse = checkAccessRight(sclBase.getAccessRightID(), requestIndication.getRequestingEntity(), Constants.AR_WRITE);
        if (errorResponse != null) {
        	em.close();
            return errorResponse;
        }
        // Check Resource Representation
        if (requestIndication.getRepresentation() == null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Resource Representation is EMPTY")) ;
        }
        // Check Attributes
        SclBase sclBaseNew = null ;  
        try{
        	sclBaseNew = (SclBase) XmlMapper.getInstance().xmlToObject(requestIndication.getRepresentation());
        } catch (ClassCastException e){
        	em.close();
        	LOGGER.debug("ClassCastException : Incorrect resource type in JAXB unmarshalling.",e);
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST, "Incorrect resource type"));
        }
        if (sclBaseNew == null){
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST, "Incorrect resource representation syntax")) ;
        }
        // Scls References Must be NP
        if (sclBaseNew.getSclsReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"sclsReference UPDATE is Not Permitted")) ;
        }
        // Applications Reference Must be NP
        if (sclBaseNew.getApplicationsReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"ApplicationsReference UPDATE is Not Permitted")) ;
        }
        // ContainersReferences Must be NP
        if (sclBaseNew.getContainersReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"ContainersReference UPDATE is Not Permitted")) ;
        }
        // GroupsReferences Must be NP
        if (sclBaseNew.getGroupsReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"GroupsReference UPDATE is Not Permitted")) ;
        }
        // AccessRightsReferences Must be NP
        if (sclBaseNew.getAccessRightsReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"AcessRightsReference UPDATE is Not Permitted")) ;
        }
        // SubscriptionsReference Must be NP
        if (sclBaseNew.getSubscriptionsReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"SubscriptionsReference UPDATE is Not Permitted")) ;
        }
        // DiscoveryReference Must be NP
        if (sclBaseNew.getDiscoveryReference() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"DiscoveryReference UPDATE is Not Permitted")) ;
        }
        // CreationTime Must be NP
        if (sclBaseNew.getCreationTime() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Creation Time UPDATE is Not Permitted")) ;
        }
        // LastModifiedTime Must be NP
        if (sclBaseNew.getLastModifiedTime() != null) {
        	em.close();
            return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST,"Last Modified Time UPDATE is Not Permitted")) ;
        }

        // Storage
        // Set accessRightID if it exists
        if (DAOFactory.getAccessRightDAO().find(sclBaseNew.getAccessRightID(), em) != null) {
            sclBase.setAccessRightID(sclBaseNew.getAccessRightID());
        }
        if (sclBaseNew.getSearchStrings() != null) {
            sclBase.setSearchStrings(sclBaseNew.getSearchStrings());
        }
        // Set LastModifiedTime
        sclBase.setLastModifiedTime(DateConverter.toXMLGregorianCalendar(new Date()).toString());

        //Notify the subscribers
        Notifier.notify(StatusCode.STATUS_OK, sclBase);

        // Store sclBase
        DAOFactory.getSclBaseDAO().update(sclBase, em);
        
        em.getTransaction().commit();
        em.close();
        
		sclBase.setAccessRightsReference(sclBase.getUri() + Refs.ACCESSRIGHTS_REF);
		sclBase.setApplicationsReference(sclBase.getUri() + Refs.APPLICATIONS_REF);
		sclBase.setContainersReference(sclBase.getUri() + Refs.CONTAINERS_REF);
		sclBase.setDiscoveryReference(sclBase.getUri() + "/discovery");
		sclBase.setGroupsReference(sclBase.getUri() + Refs.GROUPS_REF);
		sclBase.setSclsReference(sclBase.getUri() + Refs.SCLS_REF);
		sclBase.setSubscriptionsReference(sclBase.getUri() + Refs.SUBSCRIPTIONS_REF);
		
        // Response
        return new ResponseConfirm(StatusCode.STATUS_OK, sclBase);
    }

    /**
     * Deletes {@link SclBase} resource. It is not allowed Through the API
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */
    public ResponseConfirm doDelete (RequestIndication requestIndication) {

        // Response
        return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_METHOD_NOT_ALLOWED,requestIndication.getMethod()+" Method is not allowed"));
    }

    /**
     * Executes {@link SclBase} resource. It is not allowed Through the API
     * @param requestIndication - The generic request to handle.
     * @return The generic returned response.
     */
    public ResponseConfirm doExecute (RequestIndication requestIndication) {

        // Response
        return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED,requestIndication.getMethod()+" Method is not implemented"));
    }
}
