package organisation.role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import annotations.Inform;
import annotations.Workload;
import organisation.goal.GoalNode;

/**
 * @author cleber
 *
 */
public class RoleNode {
	private String roleName;
	private RoleNode parent;
	private String parentSignature; // used to find the original parent after cloning
	private List<RoleNode> descendants = new ArrayList<>();
	private Set<Workload> workloads = new HashSet<>();
	private Set<Inform> informs = new HashSet<>();
	private Set<GoalNode> assignedGoals = new HashSet<>();

	public RoleNode(RoleNode parent, String name) {
		setParent(parent);
		roleName = name;
	}
	
	public void addWorkload(Workload workload) {
		Workload w = getWorkload(workload.getId());
		if (w != null) {
			w.setValue((double) w.getValue() + (double) workload.getValue());
		} else {
			workloads.add(workload);
		}
	}
	
	public Workload getWorkload(String id) {
		for (Workload w : workloads) 
			if (w.getId().equals(id)) return w;
		
		return null;
	}
	
	public Set<Workload> getWorkloads() {
		return workloads;
	}

	public double getSumWorkload() {
		double sumEfforts = 0;
		for (Workload w : getWorkloads())
			sumEfforts += (double) w.getValue();
		return sumEfforts;
	}

	public void addInform(Inform inform) {
		Inform t = getInform(inform.getId());
		if (t != null) {
			t.setValue((double) t.getValue() + (double) inform.getValue());
		} else {
			informs.add(inform);
		}
	}

	private Inform getInform(String id) {
		for (Inform w : informs) 
			if (w.getId().equals(id)) return w;
		
		return null;
	}
	
	public Set<Inform> getInforms() {
		return informs;
	}
	
	/**
	 * sum amount of data generated by all descendants 
	 * of its parent role
	 * @return double sum of descendants of its parent
	 */
	public double getParentSumDataAmount() {
		double sumDataAmount = 0;
		if (getParent() != null) {
			for (RoleNode d : getParent().getDescendants()) {
				for (Inform t : d.getInforms())
					sumDataAmount += (double) t.getValue();
			}
		}
		return sumDataAmount;
	}
	
	/**
	 * sum amount of data generated by all descendants 
	 * @return double sum of descendants data amount
	 */
	public double getRoleSumDataAmount() {
		double sumDataAmount = 0;
		for (RoleNode d : getDescendants()) {
			for (Inform t : d.getInforms())
				sumDataAmount += (double) t.getValue();
		}
		return sumDataAmount;
	}
	
	public void assignGoal(GoalNode g) {
		assignedGoals.add(g);
	}

	public Set<GoalNode> getAssignedGoals() {
		return assignedGoals;
	}

	private void addDescendant(RoleNode newDescendant) {
		descendants.add(newDescendant);
	}

	public List<RoleNode> getDescendants() {
		return descendants;
	}

	public String getRoleName() {
		return roleName;
	}

	public RoleNode getParent() {
		return parent;
	}

	public String getParentSignature() {
		return parentSignature;
	}
	
	public void setParentSignature(String parentSignature) {
		this.parentSignature = parentSignature;
	}

	public void setParent(RoleNode parent) {
		this.parent = parent;
		if (getParent() != null) {
			setParentSignature(parent.signature());
			getParent().addDescendant(this);
		} else {
			setParentSignature("");
		}
	}

	/**
	 * Check if this role has a goal which is sibling of the given goal
	 * @param g
	 * @return
	 */
	public boolean hasSiblingGoal(GoalNode g) {
		if (getParent() == null)
			return false;
		return getParent().getAssignedGoals().contains(g.getParent());
	}

	/**
	 * Check if this role has a goal which is sibling of the given goal
	 * @param g
	 * @return
	 */
	public boolean hasParentGoal(GoalNode g) {
		return getAssignedGoals().contains(g.getParent());
	}
	
	/**
	 * Signature makes this role unique in a tree
	 * It differs from toString because signature has no reference to other nodes
	 * 
	 * @return an unique string
	 */
	public String signature() {
		String r = "";

		List<String> assignedGoals = new ArrayList<>();
		if ((getAssignedGoals() != null) && (!getAssignedGoals().isEmpty())) {
			Iterator<GoalNode> iterator = getAssignedGoals().iterator(); 
			while (iterator.hasNext()) {
				GoalNode n = iterator.next(); 
				assignedGoals.add(n.getGoalName());
			}
		}
		Collections.sort(assignedGoals);
		r += "G{" + assignedGoals + "}";

		r += "W{" + getWorkloads() + "}";
		
		r += "T{" + getInforms() + "}";

		return r;
	}
	
	public String toString() {
		String r = signature();

		if (getParent() != null) {
			r += "^";
			r += getParent().toString();
		}
		
		return r;
	}
	
	public RoleNode cloneContent() {
		// parent is not cloned it must be resolved by the tree
		RoleNode clone = new RoleNode(null, roleName);
		
		clone.setParentSignature(parentSignature);

		// descendants are not cloned, it must be resolved by the tree
		
		for (Workload s : workloads) 
			clone.workloads.add(s.clone());

		for (Inform t : informs) 
			clone.informs.add(t.clone());

		for (GoalNode goal : assignedGoals) 
			if (!clone.assignedGoals.contains(goal)) 
				clone.assignedGoals.add(goal);
		
	    return clone;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((assignedGoals == null) ? 0 : assignedGoals.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RoleNode other = (RoleNode) obj;
		if (assignedGoals == null) {
			if (other.assignedGoals != null)
				return false;
		} else if (!signature().equals(other.signature()))
			return false;
		return true;
	}
}