package deepWebSearch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

class TreeNode
{
	private TreeNode parent;
	private List<TreeNode> children;
	private double esValue; //specifity
	private int ecValue; //coverage value
	private String nodeName;
    private String fileName;
    
	public TreeNode (TreeNode parent, String name, String filename)
	{
		this.parent = parent;
		children = new ArrayList<TreeNode>();
		this.nodeName = name;
		this.fileName = filename;
	}
  
	public String getFileName()
	{
		return this.fileName;
	}
	
	public void setEsValue(double esValue)
	{
		this.esValue = esValue;
	}

	public void setEcValue(int ecValue)
	{
		this.ecValue = ecValue;
	}

	public double getEsValue()
	{
		return this.esValue;
	}

	public int getEcValue()
	{
		return this.ecValue;
	}


	public TreeNode getParent()
	{
		return this.parent;
	}

	public List<TreeNode> getChildren()
	{
		return this.children;
	}

	public void addchildren(TreeNode child)
	{
		this.children.add(child);
	}

	public void setParent(TreeNode parent)
	{
		this.parent = parent;
	}

	public String getNodeName() {
		return nodeName;
	}
}

public class webSearch {

	TreeNode root = new TreeNode(null, "Root", rootFile);
	private static final String bingSearchURL = 
			"https://api.datamarket.azure.com/Data.ashx/Bing/SearchWeb/v1/Composite?Query=%27";
	private static final String bingSearchFomart = "%27&$top=1&$format=json";
	private static final String accountKey = "hOVysMk4Ynb2GSI7COBxmjJf+GXpgKMP0xcy3RpYVY4";
	private static String accountKeyEnc;
	private static final String rootFile = "/root.txt";
	private static final String sportFile = "/sports.txt";
	private static final String healthFile = "/health.txt";
	private static final String computerFile = "/computers.txt";
	private String inputURL;
	private Map<String, TreeNode> treeNodeMapping = new HashMap<String, TreeNode>();
	private Map<String, Integer> queryCountMapping = new HashMap<String, Integer>();

	public webSearch()
	{
		byte[] accountKeyBytes = Base64.encodeBase64((accountKey + ":" + accountKey).getBytes());
		accountKeyEnc = new String(accountKeyBytes);
		root.setEsValue(1);
		queryCountMapping.put("Root", 0);
		treeNodeMapping.put("Root", root);
	}
	
	private void populateTree()
	{
		TreeNode computers = new TreeNode(root, "Computers", computerFile);
		TreeNode health = new TreeNode(root, "Health", healthFile);
		TreeNode sports = new TreeNode(root, "Sports", sportFile);
		root.addchildren(computers);
		root.addchildren(health);
		root.addchildren(sports);
		treeNodeMapping.put("Health", health);
		treeNodeMapping.put("Computers", computers);
		treeNodeMapping.put("Sports", sports);

		TreeNode disease = new TreeNode(health, "Diseases",null);
		TreeNode fitness = new TreeNode(health, "Fitness", null);
		treeNodeMapping.put("Diseases", disease);
		treeNodeMapping.put("Fitness", fitness);
		health.addchildren(disease);
		health.addchildren(fitness);
		TreeNode soccer = new TreeNode(sports, "Soccer", null);
		TreeNode basketball = new TreeNode(sports, "Basketball", null);
		treeNodeMapping.put("Soccer", soccer);
		treeNodeMapping.put("Basketball", basketball);
		sports.addchildren(soccer);
		sports.addchildren(basketball);
		TreeNode hardware = new TreeNode(computers, "Hardware", null);
		TreeNode programming = new TreeNode(computers, "Programming", null);
		treeNodeMapping.put("Hardware", hardware);
		treeNodeMapping.put("Programming", programming);
		computers.addchildren(hardware);
		computers.addchildren(programming);
	}

	private Map<String, List<String>> populateQueryMapping(TreeNode root)
	{
		Map<String, List<String>> queryMapping = new HashMap<String, List<String>>();
		String curDir = System.getProperty("user.dir");
		System.out.println("cur directory is " + curDir);
		BufferedReader br = null;
		try 
		{
			String sCurrentLine;
			br = new BufferedReader(new FileReader(curDir+root.getFileName()));
			while ((sCurrentLine = br.readLine()) != null) 
			{

				String[] afterProcessed = sCurrentLine.split("\\s+");
				if (!queryMapping.containsKey(afterProcessed[0]))
				{
					queryMapping.put(afterProcessed[0], new ArrayList<String>());
				}

				StringBuffer queryTerm = new StringBuffer();
				for (int i = 1; i < afterProcessed.length; i++)
				{
					queryTerm.append(afterProcessed[i]);
					if (i != afterProcessed.length - 1)
					{
						queryTerm.append("%20");
					}
				}
				queryMapping.get(afterProcessed[0]).add(queryTerm.toString());
			}

		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			try 
			{
				if (br != null)br.close();
			} 
			catch (IOException ex) 
			{
				ex.printStackTrace();
			}
		}
		return queryMapping;
	}

	private String classifyDataBase(TreeNode root, double esValue, int ecValue)
	{
		
		
		String result = "";
		if (root.getChildren().isEmpty())
		{
			return "/"+ root.getNodeName();
		}
		Map<String, List<String>> queryMapping = populateQueryMapping(root);
		countWebTotal(queryMapping);
        calculateSpecificity(root);
		for (TreeNode child : root.getChildren())
		{
			if (child.getNodeName().equals("Fitness"))
			{
				System.out.println("fitness ec value is " + child.getEcValue());
				System.out.println("fitness es value is " + child.getEsValue());
			}
			if (child.getEcValue() >= ecValue && child.getEsValue() >= esValue)
			{
				System.out.println("child node name is " + child.getNodeName());
				System.out.println("getEcValue is " + child.getEcValue());
				System.out.println("getEsValue is " + child.getEsValue());
				result = result + "/" + root.getNodeName() + classifyDataBase(child, esValue, ecValue);
			}
		}
		if (result.equals(""))
		{
			return "/"+root.getNodeName();
		}
		return result;
		
	}
	
	private void calculateSpecificity (TreeNode root)
	{
		  int sum = 0;
		  for (TreeNode child : root.getChildren())
		  {
			  sum += child.getEcValue();
		  }
		  for (TreeNode child : root.getChildren())
		  {
			  double specifity = ((double)root.getEsValue()*child.getEcValue())/(double)sum; //
			  child.setEsValue(specifity);
		  }
	}
	
	private  void countWebTotal(Map<String, List<String>> queryMapping)
	{
		for (Map.Entry<String, List<String>> entry: queryMapping.entrySet())
		{
			String category = entry.getKey();
			List<String> quertTermList = entry.getValue();
			int numDocs = 0;
			for (String queryTerm : quertTermList)
			{
				numDocs += getWebTotalPerQuery(queryTerm);
			}
			treeNodeMapping.get(category).setEcValue(numDocs);
		}
	}

	private int getWebTotalPerQuery(String queryTerm)
	{
		String URL = bingSearchURL + inputURL + queryTerm + bingSearchFomart;
		URL url;
		int totalDocs = 0;
		try 
		{
			url = new URL(URL);
			URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);
			InputStream inputStream = (InputStream) urlConnection.getContent();		
			byte[] contentRaw = new byte[urlConnection.getContentLength()];
			inputStream.read(contentRaw);
			String content = new String(contentRaw);
			JSONObject obj= (JSONObject) JSONValue.parse(content);
			JSONObject obj2 = (JSONObject) obj.get("d");
			JSONArray obj3 = (JSONArray) obj2.get("results");
			JSONObject obj4 = (JSONObject) obj3.get(0);
			totalDocs = Integer.valueOf((String)obj4.get("WebTotal"));
		}
		catch (MalformedURLException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return totalDocs;
	}

	public static void main(String[] args)
	{
		
		double esValue = 0.6;
		int ecValue = 100;
		webSearch inst = new webSearch();
		inst.populateTree();
		inst.inputURL = "site%3ahardwarecentral.com%20";
		String classification = inst.classifyDataBase(inst.root, esValue, ecValue);
		System.out.println("classification is " + classification);

	}
}
