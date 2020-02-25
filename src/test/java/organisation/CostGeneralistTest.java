package organisation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;

import busca.BuscaLargura;
import busca.Nodo;
import organisation.exception.CircularReference;
import organisation.exception.GoalNotFound;
import organisation.goal.GoalNode;
import organisation.goal.GoalTree;
import organisation.role.RoleNode;
import organisation.search.Cost;
import organisation.search.Organisation;
import organisation.search.Parameters;

public class CostGeneralistTest {

	@Before
	public void resetGoalTreeSingleton() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
	   Field instance = GoalTree.class.getDeclaredField("instance");
	   instance.setAccessible(true);
	   instance.set(null, null);
	}
	
    @Test
    public void testOneRoleGeneralistOrg() {
    	try {
    		System.out.println("\n\ntestOneRoleGeneralistOrg");

    		// parameters
    		Parameters.getInstance();
    		Parameters.setMaxWorkload(8.0);
    		Parameters.setWorkloadGrain(2.0);
    		System.out.println("Max workload is 8");
    		System.out.println("workload grain is 2");
    		Parameters.setMaxDataLoad(8.0);
    		Parameters.setDataLoadGrain(2.0);
    		System.out.println("Max dataload is 8");
    		System.out.println("dataloadgrain is 2");
    		
			GoalNode g0 = new GoalNode(null, "g0");
			GoalTree gTree = GoalTree.getInstance();
			gTree.setRootNode(g0);
			gTree.addGoal("g1", "g0");
			gTree.addGoal("g2", "g1");
			System.out.println("g1 must be split into two goals with 2 of workload each");
			gTree.addWorkload("g1", "w1", 4);
			System.out.println("g2 must be split into two goals with 2 of dataload each");
			gTree.addInform("g1", "i1", "g2", 4);
			gTree.brakeGoalTree();
			
			GoalNode g;
			assertNotNull(g = gTree.findAGoalByName(gTree.getRootNode(),"g0"));
			System.out.println("g0 has no inform and dataloads: " + g.getInforms() + " - " + g.getDataLoads());
			assertEquals(0, g.getSumInform(), 0);
			assertEquals(0, g.getSumDataLoad(), 0);

			assertNotNull(g = gTree.findAGoalByName(gTree.getRootNode(),"g1$0"));
			System.out.println("g1$0 ~ g1$1 all have sum of inform: " + g.getSumInform() + ", details: " + g.getInforms());
			System.out.println("g1$0 ~ g1$1 all have sum of workload: " + g.getSumWorkload() + ", details: " + g.getWorkloads());
			assertEquals(2, g.getSumWorkload(), 0);
			assertEquals(1, g.getWorkloads().size(), 0);
			assertEquals(2, g.getSumInform(), 0);
			assertEquals(2, g.getInforms().size(), 0); 
			
			assertNotNull(g = gTree.findAGoalByName(gTree.getRootNode(),"g2$0"));
			System.out.println("g2$0 ~ g2$1 all have sum of dataload: " + g.getSumDataLoad() + ", details: " + g.getDataLoads());
			assertEquals(2, g.getSumDataLoad(), 0);
			assertEquals(2, g.getDataLoads().size(), 0);

			//TODO: assert if inform was removed since it is circular
			
			System.out.println("Since thw total workload is 4, which is less than 8 (max) all goals must be assigned to an unique role.");
			Organisation o = new Organisation("generalist", gTree, Cost.GENERALIST);
			Nodo n = new BuscaLargura().busca(o);

			System.out.println("Generated rolesTree: " + ((Organisation)n.getEstado()).getRolesTree().getTree());
			for (RoleNode r : ((Organisation)n.getEstado()).getRolesTree().getTree()) {
				System.out.println("Role: " + r + ", workloads: "+r.getWorkloads() + ", informs: "+r.getInforms());
			}

			assertEquals(1, ((Organisation)n.getEstado()).getRolesTree().getTree().size());
			assertEquals(4, ((Organisation)n.getEstado()).getRolesTree().getSumWorkload(), 0);
    	} catch (CircularReference e) {
			e.printStackTrace();
		} catch (GoalNotFound e) {
			e.printStackTrace();
		}
    }
	
    
}
