package com.eyeq.pivot4j.analytics.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.olap4j.OlapException;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.MetadataElement;
import org.primefaces.component.commandbutton.CommandButton;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.eyeq.pivot4j.PivotModel;
import com.eyeq.pivot4j.analytics.ui.navigator.MemberNode;
import com.eyeq.pivot4j.analytics.ui.navigator.NodeFilter;
import com.eyeq.pivot4j.analytics.ui.navigator.SelectionNode;
import com.eyeq.pivot4j.transform.PlaceMembersOnAxes;
import com.eyeq.pivot4j.util.MemberSelection;

@ManagedBean(name = "memberSelectionHandler")
@ViewScoped
public class MemberSelectionHandler implements NodeFilter {

	@ManagedProperty(value = "#{pivotStateManager.model}")
	private PivotModel model;

	private TreeNode sourceNode;

	private TreeNode targetNode;

	private TreeNode[] sourceSelection;

	private TreeNode[] targetSelection;

	private Hierarchy hierarchy;

	private String hierarchyName;

	private CommandButton buttonAdd;

	private CommandButton buttonRemove;

	private CommandButton buttonUp;

	private CommandButton buttonDown;

	private CommandButton buttonApply;

	private CommandButton buttonOk;

	private MemberSelection selection;

	@PostConstruct
	protected void initialize() {
	}

	@PreDestroy
	protected void destroy() {
	}

	/**
	 * @return the model
	 */
	public PivotModel getModel() {
		return model;
	}

	/**
	 * @param model
	 *            the model to set
	 */
	public void setModel(PivotModel model) {
		this.model = model;
	}

	/**
	 * @return the sourceNode
	 */
	public TreeNode getSourceNode() {
		if (sourceNode == null) {
			this.sourceNode = new DefaultTreeNode();

			Hierarchy hierarchy = getHierarchy();
			if (hierarchy != null) {
				try {
					List<? extends Member> members = hierarchy.getRootMembers();

					for (Member member : members) {
						MemberNode node = new MemberNode(member);
						node.setNodeFilter(this);

						if (isVisible(member)) {
							node.setExpanded(isExpanded(member));
							node.setSelectable(isSelectable(member));
							node.setSelected(isSelected(member));
							node.getData().setSelected(isActive(member));

							sourceNode.getChildren().add(node);
						}
					}
				} catch (OlapException e) {
					throw new FacesException(e);
				}
			}
		}

		return sourceNode;
	}

	/**
	 * @param sourceNode
	 *            the sourceNode to set
	 */
	public void setSourceNode(TreeNode sourceNode) {
		this.sourceNode = sourceNode;
	}

	/**
	 * @return the targetNode
	 */
	public TreeNode getTargetNode() {
		if (targetNode == null) {
			MemberSelection selection = getSelection();

			if (selection != null) {
				this.targetNode = new SelectionNode(selection);

				targetNode.setExpanded(true);
			}
		}

		return targetNode;
	}

	/**
	 * @param targetNode
	 *            the targetNode to set
	 */
	public void setTargetNode(TreeNode targetNode) {
		this.targetNode = targetNode;
	}

	public void show() {
		reset();

		FacesContext context = FacesContext.getCurrentInstance();

		Map<String, String> parameters = context.getExternalContext()
				.getRequestParameterMap();

		this.hierarchyName = parameters.get("hierarchy");
	}

	public void reset() {
		buttonAdd.setDisabled(true);
		buttonRemove.setDisabled(true);
		buttonUp.setDisabled(true);
		buttonDown.setDisabled(true);
		buttonApply.setDisabled(true);
		buttonOk.setDisabled(true);

		this.hierarchyName = null;
		this.hierarchy = null;
		this.sourceNode = null;
		this.targetNode = null;
		this.selection = null;
	}

	public void apply() {
		PlaceMembersOnAxes transform = model
				.getTransform(PlaceMembersOnAxes.class);
		transform.placeMembers(getHierarchy(), getSelection().getMembers());

		buttonApply.setDisabled(true);
		buttonOk.setDisabled(true);
	}

	public void add() {
		FacesContext context = FacesContext.getCurrentInstance();

		Map<String, String> parameters = context.getExternalContext()
				.getRequestParameterMap();

		String modeName = parameters.get("mode");

		if (modeName == null) {
			modeName = SelectionMode.Single.name();
		}

		add(modeName);
	}

	/**
	 * @param modeName
	 */
	public void add(String modeName) {
		SelectionMode mode = null;

		if (modeName != null) {
			mode = SelectionMode.valueOf(modeName);
		}

		MemberSelection selection = getSelection();

		if (mode == null) {
			selection.clear();
		} else {
			boolean empty = true;

			List<Member> members = selection.getMembers();

			for (TreeNode node : sourceSelection) {
				MemberNode memberNode = (MemberNode) node;

				Member member = memberNode.getObject();

				List<Member> targetMembers = mode.getTargetMembers(member);

				for (Member target : targetMembers) {
					if (!members.contains(target)) {
						members.add(target);
						empty = false;
					}
				}
			}

			if (empty) {
				FacesContext context = FacesContext.getCurrentInstance();

				ResourceBundle bundle = context.getApplication()
						.getResourceBundle(context, "msg");

				String title = bundle.getString("warn.no_members.title");
				String message = bundle
						.getString("warn.no_members.select.message");
				FacesContext.getCurrentInstance().addMessage(
						null,
						new FacesMessage(FacesMessage.SEVERITY_WARN, title,
								message));
				return;
			}

			this.selection = new MemberSelection(members);
		}

		this.sourceNode = null;
		this.targetNode = null;

		this.sourceSelection = null;
		this.targetSelection = null;

		updateButtonStatus();

		buttonApply.setDisabled(false);
		buttonOk.setDisabled(false);
	}

	public void remove() {
		FacesContext context = FacesContext.getCurrentInstance();

		Map<String, String> parameters = context.getExternalContext()
				.getRequestParameterMap();

		String modeName = parameters.get("mode");

		if (modeName == null) {
			modeName = SelectionMode.Single.name();
		}

		remove(modeName);
	}

	/**
	 * @param modeName
	 */
	public void remove(String modeName) {
		SelectionMode mode = null;

		if (modeName != null) {
			mode = SelectionMode.valueOf(modeName);
		}

		MemberSelection selection = getSelection();

		if (mode == null) {
			selection.clear();
		} else {
			boolean empty = true;

			List<Member> members = selection.getMembers();

			for (TreeNode node : targetSelection) {
				SelectionNode memberNode = (SelectionNode) node;

				Member member = memberNode.getObject();

				List<Member> targetMembers = mode.getTargetMembers(member);

				for (Member target : targetMembers) {
					if (members.contains(target)) {
						members.remove(target);
						empty = false;
					}
				}
			}

			if (empty) {
				FacesContext context = FacesContext.getCurrentInstance();

				ResourceBundle bundle = context.getApplication()
						.getResourceBundle(context, "msg");

				String title = bundle.getString("warn.no_members.title");
				String message = bundle
						.getString("warn.no_members.remove.message");
				FacesContext.getCurrentInstance().addMessage(
						null,
						new FacesMessage(FacesMessage.SEVERITY_WARN, title,
								message));
				return;
			}

			this.selection = new MemberSelection(members);
		}

		this.sourceNode = null;
		this.targetNode = null;

		this.sourceSelection = null;
		this.targetSelection = null;

		updateButtonStatus();

		buttonApply.setDisabled(false);
		buttonOk.setDisabled(false);
	}

	public void moveUp() {
		SelectionNode node = (SelectionNode) targetSelection[0];
		Member member = node.getObject();

		MemberSelection selection = getSelection();
		selection.moveUp(member);

		SelectionNode parent = (SelectionNode) node.getParent();
		parent.moveUp(node);

		updateButtonStatus();

		buttonApply.setDisabled(false);
		buttonOk.setDisabled(false);
	}

	public void moveDown() {
		SelectionNode node = (SelectionNode) targetSelection[0];
		Member member = node.getObject();

		MemberSelection selection = getSelection();
		selection.moveDown(member);

		SelectionNode parent = (SelectionNode) node.getParent();
		parent.moveDown(node);

		updateButtonStatus();

		buttonApply.setDisabled(false);
		buttonOk.setDisabled(false);
	}

	public Hierarchy getHierarchy() {
		if (hierarchy == null) {
			if (hierarchyName != null && model.isInitialized()) {
				this.hierarchy = model.getCube().getHierarchies()
						.get(hierarchyName);
			}
		}

		return hierarchy;
	}

	protected MemberSelection getSelection() {
		if (selection == null) {
			Hierarchy hierarchy = getHierarchy();

			if (hierarchy != null) {
				PlaceMembersOnAxes transform = model
						.getTransform(PlaceMembersOnAxes.class);

				List<Member> members = transform.findVisibleMembers(hierarchy);
				this.selection = new MemberSelection(members);
			}
		}

		return selection;
	}

	public boolean isAddButtonEnabled() {
		boolean canAdd;

		if (sourceSelection == null || sourceSelection.length == 0) {
			canAdd = false;
		} else {
			canAdd = true;

			for (TreeNode node : sourceSelection) {
				if (((MemberNode) node).getData().isSelected()) {
					canAdd = false;
					break;
				}
			}
		}

		return canAdd;
	}

	public boolean isRemoveButtonEnabled() {
		boolean canRemove;

		if (targetSelection == null || targetSelection.length == 0) {
			canRemove = false;
		} else {
			canRemove = true;

			for (TreeNode node : targetSelection) {
				if (!((SelectionNode) node).getData().isSelected()) {
					canRemove = false;
					break;
				}
			}
		}

		return canRemove;
	}

	public boolean isUpButtonEnabled() {
		boolean canMoveUp;

		if (targetSelection == null || targetSelection.length != 1) {
			canMoveUp = false;
		} else {
			SelectionNode node = (SelectionNode) targetSelection[0];

			Member member = node.getObject();

			MemberSelection selection = getSelection();

			canMoveUp = selection.canMoveUp(member);
		}

		return canMoveUp;
	}

	public boolean isDownButtonEnabled() {
		boolean canMoveDown;

		if (targetSelection == null || targetSelection.length != 1) {
			canMoveDown = false;
		} else {
			SelectionNode node = (SelectionNode) targetSelection[0];

			Member member = node.getObject();

			MemberSelection selection = getSelection();

			canMoveDown = selection.canMoveDown(member);
		}

		return canMoveDown;
	}

	/**
	 * @param e
	 */
	public void onSourceNodeSelected(NodeSelectEvent e) {
		updateButtonStatus();
	}

	/**
	 * @param e
	 */
	public void onTargetNodeSelected(NodeSelectEvent e) {
		updateButtonStatus();
	}

	protected void updateButtonStatus() {
		buttonAdd.setDisabled(!isAddButtonEnabled());
		buttonRemove.setDisabled(!isRemoveButtonEnabled());
		buttonUp.setDisabled(!isUpButtonEnabled());
		buttonDown.setDisabled(!isDownButtonEnabled());
	}

	/**
	 * @return the hierarchyName
	 */
	public String getHierarchyName() {
		return hierarchyName;
	}

	/**
	 * @param hierarchyName
	 *            the hierarchyName to set
	 */
	public void setHierarchyName(String hierarchyName) {
		this.hierarchyName = hierarchyName;
	}

	/**
	 * @return the sourceSelection
	 */
	public TreeNode[] getSourceSelection() {
		return sourceSelection;
	}

	/**
	 * @param newSelection
	 *            the sourceSelection to set
	 */
	public void setSourceSelection(TreeNode[] newSelection) {
		if (newSelection == null) {
			this.sourceSelection = null;
		} else {
			this.sourceSelection = Arrays.copyOf(newSelection,
					newSelection.length);
		}
	}

	/**
	 * @return the targetSelection
	 */
	public TreeNode[] getTargetSelection() {
		return targetSelection;
	}

	/**
	 * @param targetSelection
	 *            the targetSelection to set
	 */
	public void setTargetSelection(TreeNode[] targetSelection) {
		this.targetSelection = targetSelection;
	}

	/**
	 * @return the buttonAdd
	 */
	public CommandButton getButtonAdd() {
		return buttonAdd;
	}

	/**
	 * @param buttonAdd
	 *            the buttonAdd to set
	 */
	public void setButtonAdd(CommandButton buttonAdd) {
		this.buttonAdd = buttonAdd;
	}

	/**
	 * @return the buttonRemove
	 */
	public CommandButton getButtonRemove() {
		return buttonRemove;
	}

	/**
	 * @param buttonRemove
	 *            the buttonRemove to set
	 */
	public void setButtonRemove(CommandButton buttonRemove) {
		this.buttonRemove = buttonRemove;
	}

	/**
	 * @return the buttonUp
	 */
	public CommandButton getButtonUp() {
		return buttonUp;
	}

	/**
	 * @param buttonUp
	 *            the buttonUp to set
	 */
	public void setButtonUp(CommandButton buttonUp) {
		this.buttonUp = buttonUp;
	}

	/**
	 * @return the buttonDown
	 */
	public CommandButton getButtonDown() {
		return buttonDown;
	}

	/**
	 * @param buttonDown
	 *            the buttonDown to set
	 */
	public void setButtonDown(CommandButton buttonDown) {
		this.buttonDown = buttonDown;
	}

	/**
	 * @return the buttonApply
	 */
	public CommandButton getButtonApply() {
		return buttonApply;
	}

	/**
	 * @param buttonApply
	 *            the buttonApply to set
	 */
	public void setButtonApply(CommandButton buttonApply) {
		this.buttonApply = buttonApply;
	}

	/**
	 * @return the buttonOk
	 */
	public CommandButton getButtonOk() {
		return buttonOk;
	}

	/**
	 * @param buttonOk
	 *            the buttonOk to set
	 */
	public void setButtonOk(CommandButton buttonOk) {
		this.buttonOk = buttonOk;
	}

	/**
	 * @param element
	 * @return
	 */
	@Override
	public <T extends MetadataElement> boolean isSelected(T element) {
		return false;
	}

	/**
	 * @param element
	 * @return
	 */
	@Override
	public <T extends MetadataElement> boolean isSelectable(T element) {
		return true;
	}

	/**
	 * @see com.eyeq.pivot4j.analytics.ui.navigator.NodeFilter#isVisible(org.olap4j.metadata.MetadataElement)
	 */
	@Override
	public <T extends MetadataElement> boolean isVisible(T element) {
		Member member = (Member) element;

		try {
			return !isActive(element) || member.getChildMemberCount() > 0;
		} catch (OlapException e) {
			throw new FacesException(e);
		}
	}

	/**
	 * @see com.eyeq.pivot4j.analytics.ui.navigator.NodeFilter#isActive(org.olap4j.metadata.MetadataElement)
	 */
	@Override
	public <T extends MetadataElement> boolean isActive(T element) {
		return getSelection().isSelected((Member) element);
	}

	/**
	 * @see com.eyeq.pivot4j.analytics.ui.navigator.NodeFilter#isExpanded(org.olap4j.metadata.MetadataElement)
	 */
	@Override
	public <T extends MetadataElement> boolean isExpanded(T element) {
		return getSelection().findChild((Member) element) != null;
	}
}
