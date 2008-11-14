/*
 *  DestroyerEntity.java
 *
 *  Project:		RiFidi Designer - A Virtualization tool for 3D RFID environments
 *  http://www.rifidi.org
 *  http://rifidi.sourceforge.net
 *  Copyright:	    Pramari LLC and the Rifidi Project
 *  License:		Lesser GNU Public License (LGPL)
 *  http://www.opensource.org/licenses/lgpl-license.html
 */
package org.rifidi.designer.library.basemodels.infrared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.rifidi.designer.entities.Entity;
import org.rifidi.designer.entities.VisualEntity;
import org.rifidi.designer.entities.databinding.annotations.MonitoredProperties;
import org.rifidi.designer.entities.gpio.GPIO;
import org.rifidi.designer.entities.gpio.GPIPort;
import org.rifidi.designer.entities.gpio.GPOPort;
import org.rifidi.designer.entities.interfaces.Field;
import org.rifidi.designer.entities.interfaces.NeedsPhysics;
import org.rifidi.designer.entities.interfaces.SceneControl;
import org.rifidi.designer.entities.interfaces.Switch;

import com.jme.bounding.BoundingBox;
import com.jme.input.InputHandler;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.input.util.SyntheticButton;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.shape.Box;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.BlendState.DestinationFunction;
import com.jme.scene.state.BlendState.SourceFunction;
import com.jme.system.DisplaySystem;
import com.jmex.physics.PhysicsNode;
import com.jmex.physics.PhysicsSpace;
import com.jmex.physics.StaticPhysicsNode;
import com.jmex.physics.contact.ContactInfo;
import com.jmex.physics.material.Material;

/**
 * @author Jochen Mader Oct 8, 2007
 * @author Dan West
 */
@MonitoredProperties(names = { "name" })
@XmlRootElement
public class InfraredEntity extends VisualEntity implements SceneControl,
		Switch, NeedsPhysics, GPIO, Field {
	/** Reference to the current physicsspace */
	private PhysicsSpace physicsSpace;
	/** Reference to the collision handler. */
	private InputHandler collisionHandler;
	/** Running state of the entity. */
	private boolean running;
	/** Empty bounding node. */
	private Node bounding;
	/** Output port of the entity */
	private GPOPort port;

	/**
	 * Constructor
	 */
	public InfraredEntity() {
		setName("Infrared");
		port = new GPOPort();
		port.setId(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.designer.entities.Entity#init()
	 */
	@Override
	public void init() {
		float len = 2.5f; // length of the trigger area

		// Create the material and alpha states for the trigger area
		MaterialState ms = DisplaySystem.getDisplaySystem().getRenderer()
				.createMaterialState();
		ms.setDiffuse(new ColorRGBA(1, 1, 1, .6f));
		BlendState as = DisplaySystem.getDisplaySystem().getRenderer()
				.createBlendState();
		as.setBlendEnabled(true);
		as.setSourceFunction(SourceFunction.SourceAlpha);
		as.setDestinationFunction(DestinationFunction.One);
		as.setEnabled(true);

		// create the trigger area
		Box irGeom = new Box("triggerSpace_geom", new Vector3f(-3 - len + 3,
				6f, 0f), len, 1.5f, .15f);
		irGeom.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
		StaticPhysicsNode triggerSpace = physicsSpace.createStaticNode();
		triggerSpace.setName("triggerSpace");
		triggerSpace.attachChild(irGeom);
		triggerSpace.setModelBound(new BoundingBox());
		triggerSpace.setRenderState(ms);
		triggerSpace.setRenderState(as);
		triggerSpace.updateModelBound();
		triggerSpace.updateRenderState();
		setNode(triggerSpace);

		prepare();

		bounding = new Node();
		bounding.setModelBound(new BoundingBox());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.designer.entities.Entity#loaded()
	 */
	@Override
	public void loaded() {
		prepare();
	}

	private void prepare() {
		// set up collisions to trigger the pusharm
		((StaticPhysicsNode) getNode()).generatePhysicsGeometry();
		((StaticPhysicsNode) getNode()).setMaterial(Material.GHOST);
		InputAction triggerAction = new InputAction() {
			public void performAction(InputActionEvent evt) {
				Node collider = ((ContactInfo) evt.getTriggerData()).getNode1()
						.equals(((StaticPhysicsNode) getNode())) ? ((ContactInfo) evt
						.getTriggerData()).getNode2()
						: ((ContactInfo) evt.getTriggerData()).getNode1();

			}
		};
		SyntheticButton intersect = ((StaticPhysicsNode) getNode())
				.getCollisionEventHandler();
		collisionHandler.addAction(triggerAction, intersect, false);
		collisionHandler.setEnabled(true);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.designer.entities.interfaces.Switch#turnOn()
	 */
	public void turnOn() {
		running = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.designer.entities.interfaces.SceneControl#start()
	 */
	public void start() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.designer.entities.interfaces.SceneControl#pause()
	 */
	public void pause() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.designer.entities.interfaces.SceneControl#reset()
	 */
	public void reset() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.designer.entities.interfaces.Switch#turnOff()
	 */
	public void turnOff() {
		running = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.designer.entities.Entity#destroy()
	 */
	@Override
	public void destroy() {
		((PhysicsNode) getNode().getChild("armPhysics")).setActive(false);
		getNode().removeFromParent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.rifidi.designer.entities.interfaces.NeedsPhysics#setCollisionHandler
	 * (com.jme.input.InputHandler)
	 */
	public void setCollisionHandler(InputHandler collisionHandler) {
		this.collisionHandler = collisionHandler;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.rifidi.designer.entities.interfaces.NeedsPhysics#setPhysicsSpace(
	 * com.jmex.physics.PhysicsSpace)
	 */
	public void setPhysicsSpace(PhysicsSpace physicsSpace) {
		this.physicsSpace = physicsSpace;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void fieldEntered(Entity entity) {
		if (isRunning()) {
			System.out.println("field entered " + entity + " at " + this);
		}
	}

	@Override
	public void fieldLeft(Entity entity) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.designer.entities.VisualEntity#setLOD(int)
	 */
	@Override
	public void setLOD(int lod) {
		// No LOD for this one.

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.designer.entities.VisualEntity#getBoundingNode()
	 */
	@Override
	public Node getBoundingNode() {
		return bounding;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.designer.entities.gpio.GPIO#getGPIPorts()
	 */
	@Override
	public List<GPIPort> getGPIPorts() {
		return Collections.emptyList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.designer.entities.gpio.GPIO#getGPOPorts()
	 */
	@Override
	public List<GPOPort> getGPOPorts() {
		List<GPOPort> portList=new ArrayList<GPOPort>();
		portList.add(port);
		return portList;
	}

}