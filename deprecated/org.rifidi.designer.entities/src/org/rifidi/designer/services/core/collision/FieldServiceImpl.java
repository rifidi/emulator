/*
 *  FieldServiceImpl.java
 *
 *  Project:		RiFidi Designer - A Virtualization tool for 3D RFID environments
 *  http://www.rifidi.org
 *  http://rifidi.sourceforge.net
 *  Copyright:	    Pramari LLC and the Rifidi Project
 *  License:		Lesser GNU Public License (LGPL)
 *  http://www.opensource.org/licenses/lgpl-license.html
 */
package org.rifidi.designer.services.core.collision;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rifidi.designer.entities.Entity;
import org.rifidi.designer.entities.SceneData;
import org.rifidi.designer.entities.VisualEntity;
import org.rifidi.designer.entities.interfaces.IField;
import org.rifidi.designer.services.core.entities.FinderService;
import org.rifidi.designer.services.core.entities.SceneDataService;
import org.rifidi.services.annotations.Inject;
import org.rifidi.services.registry.ServiceRegistry;

import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.input.util.SyntheticButton;
import com.jme.scene.Node;
import com.jmex.physics.PhysicsNode;
import com.jmex.physics.contact.ContactInfo;

/**
 * Standard implementation of the iField service.
 * 
 * @author Jochen Mader - jochen@pramari.com - Feb 4, 2008
 * 
 */
public class FieldServiceImpl implements FieldService {
	/**
	 * Logger for this class.
	 */
	private static Log logger = LogFactory.getLog(FieldServiceImpl.class);
	/**
	 * Map of currently registered iFields and their associated handlers..
	 */
	private Map<IField, InputAction> iFields;
	/**
	 * This map maps entities to the iFields they are colliding with.
	 */
	private Map<IField, Set<VisualEntity>> collisions;
	/**
	 * Currently loaded scene.
	 */
	private SceneData sceneData;
	/**
	 * Reference to the finderservice
	 */
	private FinderService finderService;

	/**
	 * Constructor.
	 */
	public FieldServiceImpl() {
		logger.debug("FieldService created");
		iFields = new HashMap<IField, InputAction>();
		collisions = new HashMap<IField, Set<VisualEntity>>();
		ServiceRegistry.getInstance().service(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.services.registry.core.collision.FieldService#registerField(org.rifidi.designer.entities.interfaces.IField)
	 */
	@Override
	public void registerField(IField iField) {
		iFields.put(iField, new ColliderInputAction(iField));
		// Might be that we are loading a new scene data and entities are
		// registering themselves, defer init until the scenedata is set.
		if (sceneData != null) {
			SyntheticButton intersect = ((PhysicsNode) ((VisualEntity) iField)
					.getNode()).getCollisionEventHandler();
			sceneData.getCollisionHandler().addAction(iFields.get(iField),
					intersect, false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.services.registry.core.collision.FieldService#unregisterField(org.rifidi.designer.entities.interfaces.IField)
	 */
	@Override
	public void unregisterField(IField iField) {
		sceneData.getCollisionHandler().removeAction(iFields.get(iField));
		iFields.remove(iField);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.services.registry.core.collision.FieldService#getCurrentFieldsList()
	 */
	@Override
	public List<IField> getCurrentFieldsList() {
		return new ArrayList<IField>(iFields.keySet());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.services.registry.core.collision.FieldService#checkFields()
	 */
	@Override
	public void checkFields() {
		for (IField iField : collisions.keySet()) {
			Set<VisualEntity> entities = collisions.get(iField);
			entities.remove(null);
			List<VisualEntity> entitiesFixed = new ArrayList<VisualEntity>(
					entities);
			for (VisualEntity ent : entitiesFixed) {
				if (ent != null
						&& !sceneData.getPhysicsSpace().collide(
								(PhysicsNode) ((VisualEntity) iField).getNode(),
								(PhysicsNode) ent.getNode())) {
					entities.remove(ent);
					iField.fieldLeft(ent);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.services.registry.core.scenedata.SceneDataChangedListener#destroySceneData(org.rifidi.designer.entities.SceneData)
	 */
	@Override
	public void destroySceneData(SceneData sceneData) {
		iFields.clear();
		collisions.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.services.registry.core.scenedata.SceneDataChangedListener#sceneDataChanged(org.rifidi.designer.entities.SceneData)
	 */
	@Override
	public void sceneDataChanged(SceneData sceneData) {
		this.sceneData = sceneData;
		for (IField iField : iFields.keySet()) {
			SyntheticButton intersect = ((PhysicsNode) ((VisualEntity) iField)
					.getNode()).getCollisionEventHandler();
			sceneData.getCollisionHandler().addAction(iFields.get(iField),
					intersect, false);
		}
	}

	/**
	 * If a collision occured this method gets executed.
	 * 
	 * @param iField
	 *            the iField the collision occured in
	 * @param collider
	 *            the colliding entity
	 */
	private void collisionDetected(IField iField, Node collider) {
		if (!collisions.containsKey(iField)) {
			collisions.put(iField, new HashSet<VisualEntity>());
		}
		Entity coll = finderService.getVisualEntityByNode(collider);
		if (coll != null
				&& !collisions.get(iField).contains((VisualEntity) coll)) {
			collisions.get(iField).add((VisualEntity) coll);
			iField.fieldEntered(coll);
		}
	}

	/**
	 * @param finderService
	 *            the finderService to set
	 */
	@Inject
	public void setFinderService(FinderService finderService) {
		logger.debug("FieldService got FinderService");
		this.finderService = finderService;
	}

	/**
	 * @param finderService
	 *            the finderService to unset
	 */
	public void unsetFinderService(FinderService finderService) {
		this.finderService = null;
	}

	/**
	 * @param sceneDataService
	 *            the sceneDataService to set
	 */
	@Inject
	public void setSceneDataService(SceneDataService sceneDataService) {
		logger.debug("FieldService got SceneDataService");
		sceneDataService.addSceneDataChangedListener(this);
	}

	/**
	 * @param sceneDataService
	 *            the sceneDataService to unset
	 */
	public void unsetSceneDataService(SceneDataService sceneDataService) {
	}

	/**
	 * An InputAction that is responsible for handling collision events.
	 * 
	 * 
	 * @author Jochen Mader - jochen@pramari.com - Feb 6, 2008
	 * 
	 */
	private class ColliderInputAction extends InputAction {
		/**
		 * The iField this action is bound to.
		 */
		private IField iField;

		/**
		 * Constructor
		 * 
		 * @param iField
		 *            the IField this action is bound to
		 */
		public ColliderInputAction(IField iField) {
			super();
			this.iField = iField;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.jme.input.action.InputActionInterface#performAction(com.jme.input.action.InputActionEvent)
		 */
		@Override
		public void performAction(InputActionEvent evt) {
			Node collider = ((ContactInfo) evt.getTriggerData()).getNode1()
					.equals(((VisualEntity) iField).getNode()) ? ((ContactInfo) evt
					.getTriggerData()).getNode2()
					: ((ContactInfo) evt.getTriggerData()).getNode1();
			collisionDetected(iField, collider);
		}

	}
}
