package organisation.search;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import busca.Estado;
import busca.Heuristica;
import organisation.OrganisationPlot;
import organisation.OrganisationStatistics;
import organisation.Parameters;
import organisation.exception.OutputDoesNotMatchWithInput;
import organisation.exception.RoleNotFound;
import organisation.goal.GoalNode;
import organisation.goal.GoalTree;
import organisation.role.RoleNode;
import organisation.role.RoleTree;
import organisation.search.cost.Cost;
import organisation.search.cost.CostResolver;
import organisation.search.cost.HeuristicResolver;
import simplelogger.SimpleLogger;

public class Organisation implements Estado, Heuristica {

	/*** STATIC ***/
	private static SimpleLogger LOG = SimpleLogger.getInstance();
	// list of target states, i.e., complete charts
	private static List<Organisation> isGoalList;
	// Cost penalty used to infer bad decisions on search
	private static CostResolver penalty;
	// Heuristic used to infer bad decisions on search
	private static HeuristicResolver heuristic;
	// Number of generated states
	private static int nStatesX2 = 0;
	// a reference to the goals tree (static)
	private static GoalTree goalsTree;
	// stop algorithm after finding the first solution
	private static boolean oneSolution = true;
	// any name for an organisation
	private static String orgName;
	
	/*** LOCAL ***/
	// the chart that is being created, potentially a complete chart
	// private List<RoleNode> rolesTree = new ArrayList<RoleNode>();
	RoleTree rolesTree = new RoleTree();
	// The goals that were not explored yet
	private List<GoalNode> goalSuccessors = new ArrayList<GoalNode>();
	// Cost supporting variables
	private int cost = 0;
	private int accCost = 0;

	public String getDescricao() {
		return "Empty\n";
	}

	public String getOrgName() {
		return orgName;
	}

	/**
	 * This constructor is used on every new state
	 */
	private Organisation() {}

	/**
	 * This constructor should be used only once for generating the root role
	 * and setup the algorithm
	 * 
	 * @param orgName is an arbitrary name for this organisation
	 * @param gTree the goal tree, supposed to be a broken tree ready to process
	 * @param costFunction the desired cost function
	 */
	public Organisation(String orgName, GoalTree gTree, Cost costFunction, Boolean oneSolution) {
		Organisation.orgName = orgName;
		Organisation.oneSolution = oneSolution;
		Organisation.nStatesX2 = 0;

		goalsTree = gTree;
		this.goalSuccessors.add(goalsTree.getRootNode());
		goalsTree.addSuccessorsToList(this.goalSuccessors, goalsTree.getRootNode());
		
		// Used to infer a bad decision on the search
		Parameters.setDefaultPenalty(this.goalSuccessors.size() + 1);
		penalty = new CostResolver(costFunction);
		heuristic = new HeuristicResolver(costFunction);
		isGoalList = new ArrayList<Organisation>();
		
		Parameters.getInstance();
		LOG.info("Max Workload  : "+ Parameters.getMaxWorkload());
		LOG.info("Workload grain: "+ Parameters.getWorkloadGrain());
		LOG.info("Max DataLoad  : "+ Parameters.getMaxDataLoad());
		LOG.info("DataLoad grain: "+ Parameters.getDataLoadGrain());
		LOG.info("Cost function : "+ costFunction);
		LOG.info("One solution? : "+ Parameters.isOneSolution());
	}

	public List<Organisation> getGoalList() {
		return isGoalList;
	}
	
	public boolean ehMeta() {
		Organisation.nStatesX2++;
		if (this.goalSuccessors.size() <= 0) {
			
			if (!isGoalList.contains(this)) {
				isGoalList.add(this);
				LOG.info("Visited #" + getNStates() + " Solution #" + isGoalList.size() + ", "
						+ this.toString() + ", Hash: " + this.hashCode() + ", Cost: " + this.accCost + "/" + this.cost);
		
				OrganisationPlot p = new OrganisationPlot();
				OrganisationStatistics s = OrganisationStatistics.getInstance();
				if (oneSolution) {
					isGoalList.clear();
					
                    final String dot = p.plotOrganisation(this, "");
        			p.saveDotAsPNG(this.getOrgName(), dot);

                    s.saveOnStatistics(this);
                    
                    return true;
				} else {
                    p.plotOrganisation(this, Integer.toString(isGoalList.size()));
					
					s.saveOnStatistics(this);

					return false;
				}
			} else {
				LOG.debug("Visited #" + getNStates() + " Duplicated solution!" + ", Hash: "
						+ this.hashCode());
			}
		}
		return false;
	}

	public boolean isValid() throws OutputDoesNotMatchWithInput {
		matchSumWorkload();
		checkNumberOfWorkloads();
		matchNumberOfGoals();

		return true;
	}
	
	private void matchSumWorkload() throws OutputDoesNotMatchWithInput {
		// checking if sum of efforts match
		if (Math.abs(goalsTree.getSumEfforts() - rolesTree.getSumWorkload()) > 0.01) {
			throw new OutputDoesNotMatchWithInput(
					"The sum of efforts of the goals tree and the created organisation does not match!");
		}
	}

	private void checkNumberOfWorkloads() throws OutputDoesNotMatchWithInput {
		// number of workloads must be equal or lower (similar workloads can be joined)
		int goalsTreeNumberOfWorkloads = 0;
		for (GoalNode g : goalsTree.getTree())
			goalsTreeNumberOfWorkloads += g.getWorkloads().size();
		int organisationNumberOfWorkloads = 0;
		for (RoleNode r : rolesTree.getTree())
			goalsTreeNumberOfWorkloads += r.getWorkloads().size();
		if (organisationNumberOfWorkloads > goalsTreeNumberOfWorkloads)
			throw new OutputDoesNotMatchWithInput("There are more workloads in the output than in the input!");
	}

	private void matchNumberOfGoals() throws OutputDoesNotMatchWithInput {
		// number of goals in the goals tree must be same as the allocated ones
		int nAssignedGoals = 0;
		for (final RoleNode or : this.getRolesTree().getTree()) nAssignedGoals += or.getAssignedGoals().size();
		GoalTree gTree = GoalTree.getInstance();
		if (nAssignedGoals != gTree.getTree().size())
			throw new OutputDoesNotMatchWithInput("There are more workloads in the output than in the input!");
	}
	
	/** Lista de sucessores */
	@SuppressWarnings("unchecked")
	public List<Estado> sucessores() {
		List<Estado> suc = new LinkedList<>(); // Lista de sucessores

		// add each goal as root
		if (rolesTree.getTree().size() == 0) {
			// add all possible successors as root
			for (GoalNode goalToBeAssociated : goalSuccessors) {
				addNotNull((List<Object>) (List<?>) suc, addRootRole(goalToBeAssociated));
			}
		} else {
			for (GoalNode goalToBeAssociated : goalSuccessors) {
				// add all children as possible successors
				for (RoleNode role : rolesTree.getTree()) {
					addNotNull((List<Object>) (List<?>) suc, addRootRole(goalToBeAssociated));
					addNotNull((List<Object>) (List<?>) suc, addRole(role, goalToBeAssociated));
					addNotNull((List<Object>) (List<?>) suc, joinRole(role, goalToBeAssociated));
				}
			}
		}

		return suc;
	}

	private void addNotNull(List<Object> l, Object e) {
		if (e != null) l.add(e);		
	}
	
	public Organisation addRootRole(GoalNode goalToAssign) {
		try {
			// Prune states with effort equal to 0
			if (goalToAssign.getSumWorkload() == 0) {
				LOG.debug("Visited #" + getNStates() + " addRootRole pruned#1 " + this.toString());
				return null;
			}

			Organisation newState = (Organisation) createState(goalToAssign);

			RoleNode nr = newState.rolesTree.createRole(null, "r" + newState.rolesTree.size(), goalToAssign);

			newState.cost = penalty.getAddRootRolePenalty(goalToAssign, this.getRolesTree(), newState.getRolesTree());
			newState.accCost = this.accCost + newState.cost;

			logTransformation("addRootRole", newState, nr);
			
			return newState;

		} catch (Exception e) {
			LOG.fatal("Fatal error on addRole! " + e.getMessage());
		}
		return null;
	}

	
	public Organisation addRole(RoleNode aGivenRole, GoalNode goalToAssign) {

		try {
			// cannot create add a role without a root
			if (this.rolesTree.size() < 1) {
				LOG.debug("Visited #" + getNStates() + "  addRole pruned#0 " + this.toString());
				return null;
			}

			// Prune states with effort equal to 0
			if (goalToAssign.getSumWorkload() == 0) {
				LOG.debug("Visited #" + getNStates() + " addRole pruned#1 " + this.toString());
				return null;
			}

			// Prune states with effort greater than max (should never happen if the goals were broken properly)
			if (goalToAssign.getSumWorkload() > Parameters.getMaxWorkload()) {
				LOG.debug("Visited #" + getNStates() + " addRole pruned#2 " + this.toString());
				return null;
			}

			// Prune states which parent cannot afford data amount
			if ((aGivenRole.getParentSumDataAmount() + aGivenRole.calculateAddedDataLoad(goalToAssign)) > Parameters.getMaxDataLoad()) {
				LOG.debug("Visited #" + getNStates() + " addRole pruned#3 " + this.toString());
				return null;
			}

			Organisation newState = (Organisation) createState(goalToAssign);

			RoleNode nr = newState.rolesTree.createRole(newState.rolesTree.findRoleByRoleName(aGivenRole.getRoleName()),
					"r" + newState.rolesTree.size(), goalToAssign);
			

			newState.cost = penalty.getAddRolePenalty(aGivenRole, goalToAssign, this.getRolesTree(), newState.getRolesTree());
			newState.accCost = this.accCost + newState.cost;

			logTransformation("addRole", newState, nr);
			
			return newState;

		} catch (RoleNotFound e) {
			LOG.fatal("Fatal error on addRole! " + e.getMessage());
		}
		return null;
	}

	public Organisation joinRole(RoleNode hostRole, GoalNode goalToAssign) {

		try {
			// cannot join a role of an empty tree
			if (this.rolesTree.size() < 1) {
				LOG.debug("Visited #" + getNStates() + " joinRole pruned#0 " + this.toString());
				return null;
			}

			// Prune states with effort equal to 0 (should never happen since a role without effort should not be created)
			if (hostRole.getSumWorkload() + goalToAssign.getSumWorkload() == 0) {
				LOG.debug("Visited #" + getNStates() + " joinRole pruned#1 " + this.toString());
				return null;
			}
			
			// Prune states with effort greater than max
			if ((hostRole.getSumWorkload() + goalToAssign.getSumWorkload()) > Parameters.getMaxWorkload()) {
				LOG.debug("Visited #" + getNStates() + " joinRole pruned#2 " + this.toString());
				return null;
			}

			// Prune states which parent cannot afford data amount 
			if ((hostRole.getParentSumDataAmount() + hostRole.calculateAddedDataLoad(goalToAssign)) > Parameters.getMaxDataLoad()) {
				LOG.debug("Visited #" + getNStates() + " joinRole pruned#3 " + this.toString());
				return null;
			}

			Organisation newState = (Organisation) createState(goalToAssign);

			RoleNode jr = newState.rolesTree.assignGoalToRoleByRoleName(hostRole.getRoleName(), goalToAssign);

			newState.cost = penalty.getJoinRolePenalty(hostRole, goalToAssign, this.getRolesTree(), newState.getRolesTree());
			newState.accCost = this.accCost + newState.cost;

			logTransformation("joinRole", newState, jr);

			return newState;
		} catch (RoleNotFound e) {
			LOG.fatal("Fatal error on joinRole! " + e.getMessage());
		}
		return null;
	}

	/**
	 * This is the signature of an organisation. Two organsiations are considered
	 * equal if they have the same name and exactly same roles tree
	 */
	public String toString() {
		return getOrgName() + rolesTree.toString() + " - " + this.goalSuccessors;
	}

	/**
	 * Verifica se um estado eh igual a outro ja inserido na lista de sucessores
	 * (usado para poda)
	 */
	public boolean equals(Object o) {
		try {
			if (o instanceof Organisation) {
				if (this.toString().equals(((Organisation) o).toString())) {
					LOG.debug("Visited #" + getNStates() + " Pruned" + this.toString() + ", Hash: "
							+ o.hashCode());
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
		if (rolesTree != null)
			return toString().hashCode();
		else
			return -1;
	}

	/**
	 * The cost to generate the individual state
	 */
	public int custo() {
		return cost;
	}

	/**
	 * Heuristic, the predicted cost to achieve the target state
	 */
	public int h() {
		try {
			return heuristic.getPedictedCost(this.goalSuccessors, this.rolesTree);
		} catch (RoleNotFound e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	/**
	 * Custo acumulado g
	 */
	public int custoAcumulado() {
		return accCost;
	}

	public Organisation createState(GoalNode gn) {

		Organisation newState = new Organisation();

		try {
			newState.rolesTree = rolesTree.cloneContent();

			// Add all successors of current state but not the new state itself
			// list of goals does not need to be cloned because does not change
			for (GoalNode goal : goalSuccessors) {
				if (goal != gn)
					newState.goalSuccessors.add(goal);
			}
		} catch (RoleNotFound e) {
			e.printStackTrace();
		}

		return newState;
	}

	public RoleTree getRolesTree() {
		return rolesTree;
	}

	private void logTransformation(String transformation, Organisation state, RoleNode role) {
		String parent = "__";
		if (role.getParent() != null)
			parent = role.getParent().getRoleName();
		LOG.trace("Visited #" + getNStates() + " " + transformation + ": " + role.getRoleName() + "^"
				+ parent + " " + state.rolesTree + ", nSucc: " + state.goalSuccessors + ", Hash: " + state.hashCode()
				+ ", Cost: " + state.accCost + "/" + state.cost);
	}
	
	/**
	 * Return the number of visited states
	 * @return an integer
	 */
	public int getNStates() {
		// visited states is incremented in EhMeta which is visited twice for each state
		// TODO: check if EhMeta is visited twice for any search algorithm
		return Organisation.nStatesX2 / 2 + 1;
	}

	/**
	 * This is the worst case scenario of created states in the search tree It is
	 * not considering any possible pruning, even the ones that occurred without
	 * constraints
	 * 
	 * Wolfram alpha input: [1 + sum {f(j,n)}, f(j,n)=Piecewise[{{{3*n}, j = 1}, {{3
	 * * f(j-1,n)}, j > 1}}]],j=1..5,n=5
	 * 
	 * @return an integer of worst case number organisations that will be created
	 */
	public long getEstimatedNumberOfOrganisations() {
        // first transformation creates an empty tree
		long nStates = 1 + goalsTree.getTree().size(); 
		for (int i = 1; i < goalsTree.getTree().size(); i++) {
			nStates += openedStates(i, goalsTree.getTree().size());
		}
		return nStates;
	}

	/**
	 * Recursively compute the number of opened states of a given number
	 * of goals considering all previously opened states
	 * 
	 * @param i the current iteration
	 * @param n the number of goals
	 * @return the number of open states
	 */
	public long openedStates(int i, int n) {    
	    long result = 0;
	    // second transformation makes each goal a root role
	    if (i == 1)
	        result = 3 * n;
	    // further transformations make 3 states from each role
	    if (i > 1) 
	        result = 3 * i * openedStates(i-1, n);
	    return result;     
	}

	public int getAdjustedEstimatedNumberOfOrganisations() {
        // second transformation makes each goal a root role
	    int openedStates = goalsTree.getTree().size();
        // first transformation creates an empty tree
	    int nStates = openedStates + 1;
	    // empirical first adjust
	    int prunedStates = -2; 

	    for (int i = 1; i < goalsTree.getTree().size(); i++) {
		    // empirical adjust of pruned nodes
	        prunedStates += openedStates * i;
	        openedStates *= i * 3;
	        nStates += openedStates - prunedStates;
	    }
	    return nStates;
	}
	
	/**
	 * This is based in a math equation obtained from the curve of the worst case
	 * The equation was generated by the Regression Calculator available in
	 * https://www.wolframalpha.com/ and http://www.xuru.org/rt/ExpR.asp for
	 * exponential model The given inputs were: {1, 2}, {2, 7}, {3, 47}, {4, 501},
	 * {5, 7174}, {6, 131147} Which were taken by observation using 1 to 5 goals for
	 * goal tree with no constraints (i.e. only workloads with effort less than the
	 * grain and sum of workloads not greater than max in order to allow all
	 * combinations) The obtained equation is: 0.0121226 * Math.exp(2.65818 *
	 * nGoals); The result is not good for 1 ~ 3 goals but for 4 goals and more has
	 * error less than 0.2% (max tested was 6)
	 * 
	 * @return an integer with approximated number organisations that will be
	 *         created in the worst case
	 */
	public int getApproximationOfNumberOfOrganisations() {
		return (int) (0.0121226 * Math.exp(2.65818 * goalsTree.getTree().size()));
	}
	
}
