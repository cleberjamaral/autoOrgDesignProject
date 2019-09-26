package organisation;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import busca.BuscaLargura;
import busca.Nodo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.apache.commons.io.FileUtils;

import simplelogger.SimpleLogger;

public class OrganisationApp {

	static List<GoalNode> tree = new ArrayList<GoalNode>();
	static Stack<GoalNode> stack = new Stack<GoalNode>();
	static GoalNode rootNode = null;
	static GoalNode referenceGoalNode = null;

	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {

		// set verbose level
		SimpleLogger.getInstance(1);

		Organisation inicial;
		if ((args.length < 1) || (args[0].equals("0"))) {
			// Sample organization
			GoalNode g0 = new GoalNode(null, "g0");
			tree.add(g0);
			GoalNode g1 = new GoalNode(g0, "g1");
			g1.addSkill("s1");
			tree.add(g1);
			GoalNode g2 = new GoalNode(g0, "g2");
			tree.add(g2);
			// GoalNode g3 = new GoalNode(g1, "g3");
			// g3.addSkill("s2");
			// GoalNode g4 = new GoalNode(g0, "g4");
			// GoalNode g5 = new GoalNode(g4, "g5");
			// g5.addSkill("s5");
			// GoalNode g6 = new GoalNode(g4, "g6");
			// g6.addSkill("s4");
			// g6.addSkill("s5");
			if (args.length == 2) {
				inicial = new Organisation(g0, Integer.parseInt(args[1]));
			} else {
				inicial = new Organisation(g0, 3);
			}
		} else {
			String file = args[0];

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new File(file));

			if (!document.getDocumentElement().getNodeName().equals("organisational-specification"))
				throw new IllegalArgumentException(
						"Error! It is expected an 'organisational-specification' XML structure");

			document.getDocumentElement().normalize();
			// Visit all possible schemes from Moise 'functional-specification'
			NodeList nList = document.getElementsByTagName("scheme");
			visitNodes(nList);

			inicial = new Organisation(rootNode, 3);
//					BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
//					System.out.print("Digite sua opcao de busca { Digite S para finalizar }\n");
//					System.out.print("\t1  -  Largura\n");
//					System.out.print("\t2  -  Profundidade\n");
//					System.out.print("\t3  -  Pronfundidade Iterativo\n");
//					System.out.print("Opcao: ");
//					str = teclado.readLine().toUpperCase();

		}

		plotOrganizationalGoalTree();

		Nodo n = null;

		n = new BuscaLargura().busca(inicial);
//	n = new BuscaProfundidade(100).busca(inicial);
//	n = new BuscaIterativo().busca(inicial);
		String solutionDepth = "[G{[g0]}S{[]}, G{[g1]}S{[s1]}^[g0][], G{[g2]}S{[]}^[g0][]] TreeSize: 3";
		if (n.getEstado().toString().equals(solutionDepth))
			System.out.println("true");
		else
			System.out.println("false");

		System.out.println(n.getEstado().toString());

	}

	private static void visitNodes(NodeList nList) {
		for (int temp = 0; temp < nList.getLength(); temp++) {

			Node node = nList.item(temp);

			if (node.getNodeType() == Node.ELEMENT_NODE) {

				Element eGoal = (Element) node;
				if (node.getNodeName().equals("goal")) {
					SimpleLogger.getInstance().debug("Node id = " + eGoal.getAttribute("id"));

					if (rootNode == null) {
						rootNode = new GoalNode(null, eGoal.getAttribute("id"));
						tree.add(rootNode);
						referenceGoalNode = rootNode;
					} else {
						GoalNode gn = new GoalNode(stack.peek(), eGoal.getAttribute("id"));
						tree.add(gn);
						referenceGoalNode = gn;
					}

				} else if (node.getNodeName().equals("plan")) {
					Element ePlan = (Element) node;
					stack.push(referenceGoalNode);
					referenceGoalNode.setOperator(ePlan.getAttribute("operator"));
					SimpleLogger.getInstance().debug(
							"Push = " + referenceGoalNode.toString() + " - Op: " + referenceGoalNode.getOperator());
				} else if (node.getNodeName().equals("skill")) {
					referenceGoalNode.addSkill(eGoal.getAttribute("id"));
					SimpleLogger.getInstance()
							.debug("Skill = " + referenceGoalNode.toString() + " : " + referenceGoalNode.getSkills());
				} else if (node.getNodeName().equals("mission")) {
					return; // end of scheme goals
				}
				if (node.hasChildNodes()) {

					visitNodes(node.getChildNodes());
					if (node.getNodeName().equals("plan")) {
						GoalNode tempGN = stack.pop();
						SimpleLogger.getInstance().debug("Poping = " + tempGN.toString());
					}
				}
			}
		}
	}

	private static void plotOrganizationalGoalTree() {
		try {
			File filepath = new File("output/diagrams");
			FileUtils.deleteDirectory(filepath);

			File file = new File("output/diagrams/tmp");
			file.getParentFile().mkdirs();
		} catch (IOException e) {}

		try (FileWriter fw = new FileWriter("output/diagrams/graph_0.gv", false);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {

			out.println("digraph G {");
			for (GoalNode or : tree) {
				if (or.getOperator().equals("parallel")) {
					out.print("\t\"" + or.getGoalName()
							+ "\" [ style = \"filled\" fillcolor = \"white\" fontname = \"Courier New\" "
							+ "shape = \"diamond\" label = <<table border=\"0\" cellborder=\"0\">"
							+ "<tr><td align=\"center\"><font color=\"black\"><b>" + or.getGoalName()
							+ "</b></font></td></tr>");
				} else {
					out.print("\t\"" + or.getGoalName()
							+ "\" [ style = \"filled\" fillcolor = \"white\" fontname = \"Courier New\" "
							+ "shape = \"ellipse\" label = <<table border=\"0\" cellborder=\"0\">"
							+ "<tr><td align=\"center\"><b>" + or.getGoalName() + "</b></td></tr>");
				}
				for (String s : or.getSkills())
					out.print("<tr><td align=\"left\"><sub><i>" + s + "</i></sub></td></tr>");
				out.println("</table>> ];");
				if (or.getParent() != null)
					out.println("\t\"" + or.getParent().getGoalName() + "\"->\"" + or.getGoalName() + "\";");
			}

			out.println("}");
		} catch (IOException e) {
		}
	}
}