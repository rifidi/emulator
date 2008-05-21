/*
 *  TagViewer.java
 *
 *  Created:	Apr 23, 2007
 *  Project:	RiFidi Emulator - A Software Simulation Tool for RFID Devices
 *  				http://www.rifidi.org
 *  				http://rifidi.sourceforge.net
 *  Copyright:	Pramari LLC and the Rifidi Project
 *  License:	Lesser GNU Public License (LGPL)
 *  				http://www.opensource.org/licenses/lgpl-license.html
 */
package org.rifidi.ui.ide.views.antennaview;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.rifidi.services.annotations.Inject;
import org.rifidi.services.registry.ServiceRegistry;
import org.rifidi.services.tags.IGen1Tag;
import org.rifidi.services.tags.impl.RifidiTag;
import org.rifidi.services.tags.registry.ITagRegistry;
import org.rifidi.services.tags.registry.ITagRegistryListener;
import org.rifidi.ui.common.reader.UIAntenna;
import org.rifidi.ui.common.reader.UIReader;
import org.rifidi.ui.common.reader.callback.TagIDChangedCallbackInterface;
import org.rifidi.ui.common.registry.RegistryChangeListener;
import org.rifidi.ui.ide.views.antennaview.model.AntennViewLabelProvider;
import org.rifidi.ui.ide.views.antennaview.model.AntennaViewContentProvider;

/**
 * This is the table where the tags will be displayed in the UI. It's setting
 * the name of the table columns, adding the drag&drop support and actions like
 * delete tag from antenna.
 * 
 * @author Jochen Mader - jochen@pramari.com
 * @author Andreas Huebner - andreas@pramari.com
 * 
 */
public class TagViewer extends TableViewer implements ITagRegistryListener,
		TagIDChangedCallbackInterface {

	private static Log logger = LogFactory.getLog(TagViewer.class);

	/**
	 * The UIReader associated with this antenna
	 */
	private UIReader reader;

	/**
	 * Reference to the Antenna of the actual reader
	 */
	private UIAntenna antenna;

	/**
	 * Window to display the widgets
	 */
	private Display display;

	/**
	 * The columns used in the the tableviewer
	 */
	private List<TableColumn> tableColumns;
	
	private ITagRegistry tagRegistry;

	/**
	 * This is the selection listener to set the sorting column
	 */
	private Listener selChangeListener = new Listener() {
		public void handleEvent(Event event) {
			// Get the effected column
			TableColumn column = (TableColumn) event.widget;
			// If the column is already sorting column change direction
			// if (getTable().getSortColumn() == column) {
			//
			// if (getTable().getSortDirection() == SWT.UP) {
			// getTable().setSortDirection(SWT.DOWN);
			// } else {
			// getTable().setSortDirection(SWT.UP);
			// }
			// } else {
			// getTable().setSortColumn(column);
			// }
			getTable().setSortColumn(column);
			// Refresh the view to update changes
			refresh();
			// TagViewer.this.getTable().setSortColumn((TableColumn)
			// event.widget);
			// TagViewer.this.refresh();
		}
	};

	public TagViewer(Composite parent, int style, UIAntenna antenna) {
		super(parent, style);
		ServiceRegistry.getInstance().service(this);
		this.antenna = antenna;
		this.reader = antenna.getReader();
		this.display = parent.getShell().getDisplay();

		// Register for Callback TagIDChanges
		reader.getReaderCallbackManager().addTagIDChangedListener(this);

		// Register as Antenna Listener
		antenna.addListener(this);
		logger.debug("Registering Listener to antenna " + antenna.getId());

		getTable().setLinesVisible(true);
		getTable().setHeaderVisible(true);

		// define the column headers
		String[] columnNames = new String[] { "", "Gen", "Data Type", "Tag ID",
				"" };
		int[] columnWidths = new int[] { 30, 50, 80, 250, 30 };
		int[] columnAlignments = new int[] { SWT.LEFT, SWT.LEFT, SWT.LEFT,
				SWT.LEFT, SWT.LEFT };

		// create columns and add listeners to them
		tableColumns = new ArrayList<TableColumn>();
		for (int i = 0; i < columnNames.length; i++) {
			TableColumn tableColumn = new TableColumn(getTable(),
					columnAlignments[i]);
			tableColumn.setText(columnNames[i]);
			tableColumn.setWidth(columnWidths[i]);
			tableColumn.addListener(SWT.Selection, selChangeListener);
			tableColumns.add(tableColumn);
		}
		// Set the Sorter to provide sorting of the Tags
		setSorter(new AntennViewTagSorter(tableColumns));
		getTable().setSortColumn(tableColumns.get(1));

		// Handle with the "DEL"-Key events in the TableViewer
		getControl().addKeyListener(new KeyListener() {

			@SuppressWarnings("unchecked")
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL) {
					List<RifidiTag> list = (List<RifidiTag>) ((IStructuredSelection) getSelection())
							.toList();
					getAntenna().removeTag(list);
				}
			}

			public void keyReleased(KeyEvent e) {
			}

		});

		setContentProvider(new AntennaViewContentProvider());
		setLabelProvider(new AntennViewLabelProvider());

		// Define the initial content of the TableViewer
		setInput(antenna);

		// create and set the celleditors
		CellEditor[] editors = new CellEditor[6];
		// add a celleditor to allow hiding of tags
		editors[0] = new CheckboxCellEditor(getTable());
		// add a celleditor to allow deletion of tags
		editors[4] = new CheckboxCellEditor(getTable());
		setCellEditors(editors);
		setColumnProperties(new String[] { "visibility", "generation",
				"datatype", "tagid", "delete" });

		setCellModifier(new ICellModifier() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object,
			 *      java.lang.String)
			 */
			public boolean canModify(Object element, String property) {
				if (property.equals("visibility")) {
					return true;
				}
				if (property.equals("delete")) {
					return true;
				}
				return false;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object,
			 *      java.lang.String)
			 */
			public Object getValue(Object element, String property) {
				if (property.equals("visibility")) {
					return ((RifidiTag) element).isVisbile;
				}
				if (property.equals("delete")) {
					return true;
				}
				return null;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object,
			 *      java.lang.String, java.lang.Object)
			 */
			public void modify(Object element, String property, Object value) {
				// change the visibility of a tag
				if (property.equals("visibility")) {
					logger.debug("Visibility change detected");
					RifidiTag tag = (RifidiTag) ((TableItem) element).getData();
					ArrayList<RifidiTag> tagList = new ArrayList<RifidiTag>();
					tagList.add(tag);
					if ((Boolean) value == true) {
						getAntenna().enableTag(tagList);
					} else {
						getAntenna().disableTag(tagList);
					}
					// refresh();
				}
				// delete a tag
				if (property.equals("delete")) {
					RifidiTag tag = (RifidiTag) ((TableItem) element).getData();
					List<RifidiTag> taglist = new ArrayList<RifidiTag>();
					taglist.add(tag);
					getAntenna().removeTag(taglist);
					logger.debug("Tag Remove  " + tag.toString());
					// refresh();
				}
			}

		});

		/*
		 * Drag&Drop support is only working with Strings. When a tag is dropped
		 * in the tableViewer we get the ID of the Tag dropped in the Table.
		 * Then we use the TagRegistry to get the real tag object.
		 */
		addDropSupport(DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT,
				new Transfer[] { TextTransfer.getInstance() },
				new DropTargetListener() {

					/*
					 * (non-Javadoc)
					 * 
					 * @see org.eclipse.swt.dnd.DropTargetListener#dragEnter(org.eclipse.swt.dnd.DropTargetEvent)
					 */
					public void dragEnter(DropTargetEvent event) {
					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see org.eclipse.swt.dnd.DropTargetListener#dragLeave(org.eclipse.swt.dnd.DropTargetEvent)
					 */
					public void dragLeave(DropTargetEvent event) {
					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see org.eclipse.swt.dnd.DropTargetListener#dragOperationChanged(org.eclipse.swt.dnd.DropTargetEvent)
					 */
					public void dragOperationChanged(DropTargetEvent event) {
					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see org.eclipse.swt.dnd.DropTargetListener#dragOver(org.eclipse.swt.dnd.DropTargetEvent)
					 */
					public void dragOver(DropTargetEvent event) {
					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see org.eclipse.swt.dnd.DropTargetListener#drop(org.eclipse.swt.dnd.DropTargetEvent)
					 */
					public void drop(DropTargetEvent event) {
						LinkedList<RifidiTag> list = new LinkedList<RifidiTag>();
						String text = (String) event.data;
						String[] textArray = text.split("\n");
						for (String i : textArray) {
							RifidiTag tag = tagRegistry.getTag(Long.parseLong(i));
							list.add(tag);
							logger.debug("Drag and Drop " + i);
						}
						getAntenna().addTag(list);
						refresh();
					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see org.eclipse.swt.dnd.DropTargetListener#dropAccept(org.eclipse.swt.dnd.DropTargetEvent)
					 */
					public void dropAccept(DropTargetEvent event) {
					}

				});
		tagRegistry.addListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.ide.registry.RegistryChangeListener#add(org.rifidi.ide.registry.RegistryEvent)
	 */
	public void addEvent(List<RifidiTag> tags) {
		logger.debug("recived add Event");
		try {
			refresh();
		} catch (SWTException e) {
			logger.debug(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.ide.registry.RegistryChangeListener#remove(org.rifidi.ide.registry.RegistryEvent)
	 */
	public void removeEvent(List<RifidiTag> tags) {
		logger.debug("recived remove Event ");
		try {
			refresh();
		} catch (SWTException e) {
			logger.debug(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.ide.registry.RegistryChangeListener#update(org.rifidi.ide.registry.RegistryEvent)
	 */
	public void modifyEvent(List<RifidiTag> tags) {
		logger.debug("recived update Event");
		try {
			refresh();
		} catch (SWTException e) {
			logger.debug(e);
		}
	}

	/**
	 * This are the Enum's for the table columns
	 * 
	 */
	public enum Columns {
		VISUAL, GEN, DATATYPE, TAGID, QUALITY, DELETE
	};

	/**
	 * Unregister the Listener this Antenna is associated with
	 */
	public void dispose() {
		tagRegistry.removeListener(this);
	}

	/**
	 * @return the UIReader associated with this AntennaView
	 */
	public UIReader getReader() {
		return reader;
	}

	/**
	 * @return the UIAntenna associated with this AntennaView
	 */
	public UIAntenna getAntenna() {
		return antenna;
	}

	// public TableViewer getTableViewer() {
	// return this;
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.ui.common.reader.callback.TagIDChangedCallbackInterface#tagIDChanged(byte[],
	 *      org.rifidi.emulator.tags.Gen1Tag)
	 */
	public void tagIDChanged(Long tagEntityID, final IGen1Tag tag) {
		logger.debug("TagID has changed .. plz update");
		display.syncExec(new Runnable() {

			public void run() {
				// update will refresh the view and get a new TagList
				modifyEvent(null);
			}

		});
	}
	
	@Inject
	public void setTagRegistryService(ITagRegistry tagRegisrty)
	{
		this.tagRegistry = tagRegisrty;
		
	}

}
