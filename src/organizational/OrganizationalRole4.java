package organizational;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import busca.Antecessor;
import busca.Estado;

import simplelogger.SimpleLogger;

public class OrganizationalRole4 implements Estado, Antecessor {

	/*** STATIC ***/
	private static SimpleLogger LOG = SimpleLogger.getInstance(1);
	// list of target states, i.e., complete charts
	private static List<OrganizationalRole4> isGoalList = new ArrayList<OrganizationalRole4>();

	/*** LOCAL ***/
	// this is the chart that is being created by the algorithm, potentially a complete chart
	private List<OrganizationalRole4> rolesTree = new ArrayList<OrganizationalRole4>();
	// The goals that were not explored yet, the algorithm end when all goals were allocated into roles
	private List<GoalNode> goalSuccessors = new ArrayList<GoalNode>();
	
	String roleName = "";
	// list of goals that this role is assigned to achieve
	private List<GoalNode> assignedGoals = new ArrayList<GoalNode>();
	
	// State description
	private OrganizationalRole4 parentRole;
	private Set<String> roleSkills = new HashSet<String>();
	// graphlinks is a list because it is used as hash and it allows multiple identical links when joining function is used
	private List<String> graphLinks = new ArrayList<String>();
	
	// the orgRoleId is a reference, like an index for this state (node of the orgchart), other goals can be assigned besides the head
	GoalNode orgRoleId;
	// orgRoleIdLinks is a graph connecting ids, which leads to unique structures allowing to prune similar ones
	private List<String> orgRoleIdLinks = new ArrayList<String>();
	
	// Cost supporting variables
	private int flatCost = 0;
	private int divisionalCost = 0;
	
	/**
	 * 1: unitary cost (no function)
	 * 2: based on flat cost (flatter structures are more expensive)
	 * 3: based on divisional cost (fewer divisions means the divisions are more populated and expensive) 
	 */
	static int costFunction = 1;

	public String getDescricao() {
		return "Empty\n";
	}

	public OrganizationalRole4(GoalNode gn, int costFunction) {
		this(gn);
		
		OrganizationalRole4.costFunction = costFunction;
	}

	public OrganizationalRole4(GoalNode gn) {
		orgRoleId = gn;
		assignedGoals.add(gn);

		if (gn.getParent() == null) {
			for (GoalNode goal : gn.getSuccessors())
				goalSuccessors.add(goal);

			roleName = "r"+this.rolesTree.size();
			rolesTree.add(this);
		}
	}

	public boolean ehMeta() {

		if (goalSuccessors.size() <= 0) 
		{
			if (!isGoalList.contains(this)) {
				isGoalList.add(this);
				LOG.info("GOAL ACHIEVED! Solution: #" + isGoalList.size() + " : " + this.rolesTree + " : " + this.graphLinks + " | " + this.hashCode());

				try (FileWriter fw = new FileWriter("graph_" + isGoalList.size() + ".gv", false);
						BufferedWriter bw = new BufferedWriter(fw);
						PrintWriter out = new PrintWriter(bw)) {
					out.println("digraph G {");
					for (OrganizationalRole4 or : rolesTree) {
						out.print("\t\"" + or.roleName//headGoal.getGoalName()
								+ "\" [ style = \"filled\" fillcolor = \"white\" fontname = \"Courier New\" "
								+ "shape = \"Mrecord\" label = <<table border=\"0\" cellborder=\"0\" bgcolor=\"white\">"
								+ "<tr><td bgcolor=\"black\" align=\"center\"><font color=\"white\">"
								+ or.roleName + "</font></td></tr><tr><td align=\"center\">" + or.assignedGoals + "</td></tr>");
						for (String s : or.roleSkills)
							out.print("<tr><td align=\"left\">" + s + "</td></tr>");
						out.println("</table>> ];");
					}

					for (String s : this.graphLinks)
						out.println("\t" + s + ";");
					out.println("}");
				} catch (IOException e) {
				}
				
				//return true; // if only one solution is needed
			} else {
				// This should not happen again, it has occurred because searching process
				// (deepth) was calling ehMeta 2 times
				LOG.warn("Goal achieved but duplicated!" + " : " + this.hashCode());
				//return true; // if only one solution is needed
			}
		}
		return false;
	}

	public OrganizationalRole4 getRootRole(OrganizationalRole4 node) {
		if (node.parentRole == null)
			return node;
		else
			return getRootRole(node.parentRole);
	}

	public int custo() {
		if (costFunction == 2) {
			//LOG.debug("flatCost: " + flatCost);
			return flatCost;
		}
		else if (costFunction == 3) {
			//LOG.debug("divisionalCost: " + divisionalCost);
			return divisionalCost;
		} else {
			// default cost function is unitary (any openning has same cost)
			return 1; 
		}
	}

	/** Lista de sucessores */
	public List<Estado> sucessores() {
		List<Estado> suc = new LinkedList<Estado>(); // Lista de sucessores

		if (!goalSuccessors.isEmpty())
			LOG.debug("\nCURRENT HEAD GOAL DATA: " + this.toString() + " - OpenGoals: [" + goalSuccessors.toString() + "] - Size: "
					+ goalSuccessors.size() + ", Name: " + this.roleName + ", Hash: " + this.hashCode());

		// add all children as possible successors
		for (GoalNode goalToBeAssociated : goalSuccessors) {
			
			// if one of assigned goals is parent, so open it as a child
			if (this.assignedGoals.contains(goalToBeAssociated.getParent())) {
				// creating successors, create a subordinate role (child)
				addSubordinate(this, suc, goalToBeAssociated);
				// creating successors, join goals of a pair
				if (((this.roleSkills.containsAll(goalToBeAssociated.getSkills())
						// && !goalToBeAssociated.getParent().getOperator().equals("parallel") //does it
						// makes sense?
						) || (goalToBeAssociated.getSkills().isEmpty()))) {
					try {
						joinASubordinate(suc, goalToBeAssociated);
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
				} 
			} else if ((this.parentRole != null) && (this.parentRole.assignedGoals.contains(goalToBeAssociated.getParent()))) {
				// creating successors, create a pair role (sibling)
				addPair(suc, goalToBeAssociated);

				// creating successors, join goals of a pair
				if (((this.roleSkills.containsAll(goalToBeAssociated.getSkills())
						// && !goalToBeAssociated.getParent().getOperator().equals("parallel") //does it
						// makes sense?
						) || (goalToBeAssociated.getSkills().isEmpty()))) {
					try {
						joinAPair(suc, goalToBeAssociated);
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
				} 
			} else {
				addANotRelative(suc, goalToBeAssociated);
			}
		}

		return suc;
	}

	public void addSubordinate(OrganizationalRole4 parentRole, List<Estado> suc, GoalNode goalToBeAssociatedToRole) {

		OrganizationalRole4 newState = (OrganizationalRole4) createState(parentRole, goalToBeAssociatedToRole);
		newState.roleName = "r"+newState.rolesTree.size();
		newState.rolesTree.add(newState);
		
		newState.orgRoleIdLinks.add("\""+newState.parentRole.orgRoleId.getGoalName() + "\"->\"" + goalToBeAssociatedToRole.getGoalName()+"\"");
		newState.graphLinks.add("\""+newState.parentRole.roleName + "\"->\"" + newState.roleName+"\"");

		suc.add(newState);

		LOG.debug("addSubordinate: " + newState.rolesTree + ", nSucc: " + newState.goalSuccessors.size() + ", Name: " + newState.roleName + ", Parent: " + newState.parentRole.roleName + ", Hash: " + newState.hashCode());
	}

	public void addPair(List<Estado> suc, GoalNode goalToBeAssociatedToRole) {

		OrganizationalRole4 newState = (OrganizationalRole4) createState(this.parentRole, goalToBeAssociatedToRole);
		newState.roleName = "r"+newState.rolesTree.size();
		newState.rolesTree.add(newState);
		
		newState.orgRoleIdLinks.add("\""+newState.parentRole.orgRoleId.getGoalName() + "\"->\"" + goalToBeAssociatedToRole.getGoalName()+"\"");
		newState.graphLinks.add("\""+newState.parentRole.roleName + "\"->\"" + newState.roleName+"\"");

		suc.add(newState);

		LOG.debug("addPair       : " + newState.rolesTree + ", nSucc: " + newState.goalSuccessors.size() + ", Name: " + newState.roleName + ", Parent: " + newState.parentRole.roleName + ", Hash: " + newState.hashCode());
	}
	
	public void joinAPair(List<Estado> suc, GoalNode goalToBeAssociatedToRole) throws CloneNotSupportedException {

		OrganizationalRole4 newState = (OrganizationalRole4) createState(this.parentRole, goalToBeAssociatedToRole);
		
		// this organization is being compressed in few divisions, so division cost increased
		newState.divisionalCost = this.divisionalCost + 1;

		// the new role is also assigned to a new goal (the joined one)
		for (OrganizationalRole4 or : newState.rolesTree) {
			if (or.assignedGoals.containsAll(this.assignedGoals)) {
				if (!or.assignedGoals.contains(goalToBeAssociatedToRole)) or.assignedGoals.add(goalToBeAssociatedToRole);
				// create a link which is same as another existing, in fact it will only change the hashcode of this state
				newState.orgRoleIdLinks.add("\""+newState.parentRole.orgRoleId.getGoalName() + "\"->\"" + or.orgRoleId.getGoalName()+"\"");
				newState.graphLinks.add("\""+newState.parentRole.roleName + "\"->\"" + or.roleName+"\"");
				break;
			}
		}

		suc.add(newState);

		LOG.debug("joinAPair     : " + newState.rolesTree + ", nSucc: " + newState.goalSuccessors.size() + ", Name: " + newState.roleName + ", Parent: " + newState.parentRole.roleName + ", Hash: " + newState.hashCode());
	}

	public void joinASubordinate(List<Estado> suc, GoalNode goalToBeAssociatedToRole) throws CloneNotSupportedException {

		OrganizationalRole4 newState = (OrganizationalRole4) createState(this.parentRole, goalToBeAssociatedToRole);
		
		// the new role is also assigned to a new goal (the joined one)
		for (OrganizationalRole4 or : newState.rolesTree) {
			if (or.assignedGoals.containsAll(this.assignedGoals)) {
				if (!or.assignedGoals.contains(goalToBeAssociatedToRole)) or.assignedGoals.add(goalToBeAssociatedToRole);
				// create a link which is same as another existing, in fact it will only change the hashcode of this state
				newState.orgRoleIdLinks.add("\""+or.orgRoleId.getGoalName() + "\"->\"" + or.orgRoleId.getGoalName()+"\"");
				newState.graphLinks.add("\""+or.roleName + "\"->\"" + or.roleName+"\"");
				break;
			}
		}

		suc.add(newState);

		LOG.debug("joinASubordinate     : " + newState.rolesTree + ", nSucc: " + newState.goalSuccessors.size() + ", Name: " + newState.roleName + ", Parent: " + newState.parentRole.roleName + ", Hash: " + newState.hashCode());
	}

	public void addANotRelative(List<Estado> suc, GoalNode goalToBeAssociatedToRole) {
		// Find a role that contains the parent goal of goalToBeAssociatedToRole, it intents
		// to start to explore another branch of the tree
		for (OrganizationalRole4 or : this.rolesTree) {
			if (or.assignedGoals.contains(goalToBeAssociatedToRole.getParent())) {
				LOG.debug("addANotRelative");
				addSubordinate(or, suc, goalToBeAssociatedToRole);
				break;
			}
		}
	}
	
	/** Lista de antecessores, para busca bidirecional */
	public List<Estado> antecessores() {
		return sucessores();
	}

	public String toString() {
		String r = "{"+this.roleName+":";
		if ((this.assignedGoals != null) && (!this.assignedGoals.isEmpty())) r += "Goals[" + this.assignedGoals + "]";
		if ((this.roleSkills != null) && (!this.roleSkills.isEmpty())) r += " Skills[" + this.roleSkills + "]";
		if ((this.graphLinks != null) && (!this.graphLinks.isEmpty())) r += " Links[" + this.graphLinks + "]";
		r += "} ";
		return r;
		
	}

	/**
	 * Verifica se um estado eh igual a outro ja inserido na lista de sucessores
	 * (usado para poda)
	 */
	public boolean equals(Object o) {
		try {
			if (o instanceof OrganizationalRole4) {
				Collections.sort(this.orgRoleIdLinks);
				Collections.sort(((OrganizationalRole4) o).orgRoleIdLinks);
				if (this.orgRoleIdLinks.equals(((OrganizationalRole4) o).orgRoleIdLinks)) {
					LOG.debug("Pruned" + this.orgRoleIdLinks + " - " + ((OrganizationalRole4) o).orgRoleIdLinks);
					return true;
				}
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * retorna o hashCode desse estado (usado para poda, conjunto de fechados)
	 */

	public int hashCode() {
		if ((this.graphLinks != null) && (this.rolesTree != null))
			return this.graphLinks.hashCode() + this.rolesTree.toString().hashCode();
		else
			return -1;
	}

	/**
	 * Custo acumulado g
	 */
	public int custoAcumulado() {
		return 0; 
	}
	
	public OrganizationalRole4 clone() {
		// Only the tree is not copied because of recursiveness
		OrganizationalRole4 clone = new OrganizationalRole4(this.orgRoleId);
		for (String skill : this.roleSkills) clone.roleSkills.add(skill);
		clone.parentRole = this.parentRole;
		clone.roleName = this.roleName;
		for (String s : this.graphLinks) clone.graphLinks.add(s);
		for (GoalNode goal : this.goalSuccessors) clone.goalSuccessors.add(goal);
		// copy assigned goals - list of goals does not need to be cloned because does not change
		for (GoalNode goal : this.assignedGoals) {
			if (!clone.assignedGoals.contains(goal)) clone.assignedGoals.add(goal);
		}
		
	    return clone;
	}

	public OrganizationalRole4 createState(OrganizationalRole4 parentRole, GoalNode gn) {

		// the in factum parent may be changed if it was joined in another role
		OrganizationalRole4 infactumParent = parentRole;
		// new state
		OrganizationalRole4 newState = new OrganizationalRole4(gn);
		// Copy all skills of the goal to this new role
		for (String skill : gn.getSkills()) newState.roleSkills.add(skill);
		// Copy all graph links tree
		for (String s : this.graphLinks) newState.graphLinks.add(s);
		// Copy all roles tree
		System.out.println(rolesTree);
		for (OrganizationalRole4 or : rolesTree) {
			OrganizationalRole4 nnewS = (OrganizationalRole4) or.clone();  			
			newState.rolesTree.add((OrganizationalRole4) nnewS);
			// the new role is also assigned to a new goal (the joined one)
			if (or.assignedGoals.contains(gn.getParent())) {
				infactumParent = or;
				System.out.println(or.roleName+" - "+gn.getParent()+" - "+or.assignedGoals);
			}
		}
		// Make the parent of this state being parent of the new one (they are siblings) and copy current links
		newState.parentRole = infactumParent;
		// Add all successors of current state but not the new state itself - list of goals does not need to be cloned because does not change
		for (GoalNode goal : this.goalSuccessors) {
			if (goal != gn) newState.goalSuccessors.add(goal);
		}
		
	    return newState;
	}
	
}