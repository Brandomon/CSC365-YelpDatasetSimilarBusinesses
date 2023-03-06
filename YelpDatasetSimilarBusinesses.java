//*************************************************************************************************
//
//	Brandon LaPointe & Param Rajguru
//	CSC365 - Professor Doug Lea
//	YelpDatasetSimilarBusinesses.java
//
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import com.google.gson.Gson;	// Uses GSON 2.8.2 jar to parse JSON

public class YelpDatasetSimilarBusinesses implements ActionListener {
	
	// Class Constants & Variables
	private static final int MAX_NUM = 10_000;														// Maximum number of businesses to add to object array
	private static final int NUM_SIMILAR_BUSINESSES = 2;											// Number of similar businesses fetched from array
	private static final int MAX_SEARCH_CATEGORIES = 64;											// Maximum number of categories within a single business for array sizing
	private static String fileAddress = "yelp_dataset_business.json";								// Address of JSON file used (Yelp Academic Dataset Business)
	private static Business[] businesses;															// Array of business objects
	private static ArrayList<String> catAryList = new ArrayList<String>();							// Expandable array list containing all of the categories from all businesses
	private static ArrayList<String[]> catAryAryList = new ArrayList<String[]>();					// Array list containing string arrays of categories from all businesses
	private static String[] searchResultNames = new String[NUM_SIMILAR_BUSINESSES];					// Array containing the search result of the 3 most similar business names
	private static String[] searchResultAddresses = new String[NUM_SIMILAR_BUSINESSES];				// Array containing the search result of the 3 most similar business addresses
	
	// Hashmaps
	private static HashMap <String, String> bName_bId = new HashMap <String, String>();				// Hashmap of business name to business id
	private static HashMap <String, String> bId_bName = new HashMap <String, String>();				// Hashmap of business id to business name
	private static HashMap <String, String[]> bId_category = new HashMap <String, String[]>();		// Hashmap of business id to business categories
	private static HashMap <Integer, String> index_bId = new HashMap <Integer, String>();			// Hashmap of index to business id
	private static HashMap <String, Integer> bId_index = new HashMap <String, Integer>();			// Hashmap of business id to index
	private static HashMap <String, Double> category_tfidf = new HashMap <String, Double>();		// Hashmap of categories to tfidf
	private static HashMap <String, String> bId_address = new HashMap <String, String>();			// Hashmap of business id to address
	
	// GUI objects
	private static JLabel titleBox;				// Title box
	private static JLabel searchLabel;			// Search box label
	private static JTextField searchText;		// Search Text
	private static JButton button;				// Search button
	private static JLabel responseName;			// Result name 1 of similarity search
	private static JLabel responseName2;		// Result name 2 of similarity search
	private static JLabel responseAddress;		// Result address 1 of similarity search
	private static JLabel responseAddress2;		// Result address 2 of similarity search
	
	//****************************************************************************************************
	//
	// WordFrequencyTable Class
	//
	public class WordFrequencyTable implements java.io.Serializable {
		private static final long serialVersionUID = 1L;
		
		//*********************************************
		// Node Class
		static final class Node {
			String key;
			Node next;
			int count;
			Node(String k, int c, Node n) { key = k; count = c; next = n;}
		}
		
		//*********************************************
		// Instantiate Node Array
		static Node[] table = new Node[8]; // Always a power of 2
		static int size = 0;
		
		//*********************************************
		// GetCount Function
		static int getCount(String key) {
			int h = key.hashCode();
			int i = h & (table.length - 1);						// i = bitwise AND on h using table.length-1 (like a bit-mask, to return only the low-order bits of h. Basically a super-fast variant of h % table.length)
			for (Node e = table[i]; e != null; e = e.next) {
				if (key.equals(e.key)) {
					return e.count;
				}
			}
		return 0;
		}
		
		//*********************************************
		// Add Function
		static void add(String key) {
			int h = key.hashCode();
			int i = h & (table.length - 1);
			for (Node e = table[i]; e != null; e = e.next) {
				if (key.equals(e.key)) {
					++e.count;
					return;
				}
			}
			table[i] = new Node(key, 1, table[i]);
			++size;
			if ((float)size/table.length >= 0.75f) {
				resize();
				System.out.println("Table resized to size : " + table.length);	//======================================================================SYSTEM OUTPUT
			}
		}
		
		//*********************************************
		// Resize Function
		static void resize() {
			Node[] oldTable = table;
			int oldCapacity = oldTable.length;
			int newCapacity = oldCapacity << 1;
			Node[] newTable = new Node[newCapacity];
			for (int i = 0; i < oldCapacity; ++i) {
				for (Node e = oldTable[i]; e != null; e = e.next) {
					int h = e.key.hashCode();
					int j = h & (newTable.length - 1);
					newTable[j] = new Node(e.key, e.count, newTable[j]);
				}
			}
			table = newTable;			
		}
		
		//*********************************************
		// Remove Function
		static void remove(String key) {
			int h = key.hashCode();
			int i = h & (table.length - 1);
			Node e = table[i], p = null;
			while (e != null) {
				if (key.equals(e.key)) {
					if (p == null) {
						table[i] = e.next;
					}
					else {
						p.next = e.next;
					}
					break;
				}
				p = e;
				e = e.next;
			}
		}
		
		//*********************************************
		// PrintAll Function
		static void PrintAll() {
			for (int i = 0; i < table.length; ++i)
				for (Node e = table[i]; e != null; e = e.next) {
					System.out.println("Key : " + e.key + " --- Count : " + e.count);
				}
		}
	}
	
	//****************************************************************************************************
	//
	// Business Object Class
	//
	public final class Business {		
		// Object Variables
		private String business_id;
		private String name;
		private String address;
		private String categories;
		
		//*********************************************
		// Getters and setters
		public String getId() {
			return business_id;
		}
		public void setId(String business_id) {
			this.business_id = business_id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getAddress() {
			return address;
		}
		public void setAddress(String address) {
			this.address = address;
		}
		public String getCategories() {
			return categories;
		}
		public void setCategories(String categories) {
			this.categories = categories;
		}
	}
	
	//****************************************************************************************************
	//
	//	main
	//		Contains GUI which takes in a business name to search for similar businesses using the
	//		similarBusinesses similarity metric and displays the three most similar businesses.
	//
	public static void main(String[] args) {
			
		// Setup GUI
		setupGUI();
		
		// Convert JSON file to business object array
		jsonToObjAry();
		
		// Populate hashmaps using businesses object array
		populateHashmaps();
		
		//****************************************************************************************************
		
		// Test - Print word frequency table
		//WordFrequencyTable.PrintAll();
		
		// Test - JsonToObjAry
		/*
		System.out.println("Business 406 ID : " + businesses[406-1].getId());
		System.out.println("Business 406 Name : " + businesses[406-1].getName());
		System.out.println("Business 406 Categories : " + businesses[406-1].getCategories());
		System.out.println("Business 406 Categories : " + Arrays.toString(businesses[406-1].getCategories().split(", ")));
		*/
		
		// Test - PopulateHashmaps
		/*
		System.out.println("Category Array List : " + catAryList);
		System.out.println("bName -> bId : " + bName_bId.get("Target"));
		System.out.println("bId -> bName : " + bId_bName.get("rwsVxJqln-0HZFg1zxWtwQ"));
		System.out.println("bId -> category : " + Arrays.toString(bId_category.get("rwsVxJqln-0HZFg1zxWtwQ")));
		System.out.println("index -> bId : " + index_bId.get(10000 - 1));
		System.out.println("bId -> index : " + bId_index.get("rwsVxJqln-0HZFg1zxWtwQ"));
		System.out.println("category -> tfidf : " + category_tfidf.get("Department Stores"));
		System.out.println("bName -> bId => bId -> category : " + Arrays.toString(bId_category.get(bName_bId.get("Target"))));
		*/
		
		// Test - TfIdf
		/*
		System.out.println("TfIdf of Food : " + tfIdf(catAryList, catAryAryList, "Food"));		
		System.out.println("TfIdf of Burgers : " + tfIdf(catAryList, catAryAryList, "Burgers"));		
		System.out.println("TfIdf of Department Stores : " + tfIdf(catAryList, catAryAryList, "Department Stores"));		
		System.out.println("TfIdf of Fashion : " + tfIdf(catAryList, catAryAryList, "Fashion"));		
		System.out.println("TfIdf of Shipping Centers : " + tfIdf(catAryList, catAryAryList, "Shipping Centers"));
		*/
		
		//****************************************************************************************************
	}
	
	//****************************************************************************************************
	//
	//	actionPerformed
	//		Code run when search button is pressed to search for similar businesses.
	//
	public void actionPerformed(ActionEvent e) {
		
		// Convert searchText box input to string
		String search = searchText.getText();
		responseName.setText("");
		responseName2.setText("");
		responseAddress.setText("");
		responseAddress2.setText("");
		
		// If search field is empty display error message on GUI
		if (search.equalsIgnoreCase("")) {
			responseName.setText("ERROR - Empty Search Text Field");
			responseName2.setText("");
			responseAddress.setText("");
			responseAddress2.setText("");
		}
		// Else if search field contains characters
		else if (!search.equalsIgnoreCase("")) {
			// If searched business name is found within businesses array
			if (validName(search, businesses) == true) {
				// Search for similar businesses
				searchResultNames = searchBusinesses(search);
				searchResultAddresses = getSimilarAddresses(search);				
				// Display the results on GUI
				responseName.setText("Name : " + searchResultNames[0]);
				responseName2.setText("Name : " + searchResultNames[1]);
				responseAddress.setText("Address : " + searchResultAddresses[0]);
				responseAddress2.setText("Address : " + searchResultAddresses[1]);
			}
			// Else business name not found within businesses array
			else {
				// Display error message on GUI
				responseName.setText("ERROR - Business not found within dataset");
				responseName2.setText("");
				responseAddress.setText("");
				responseAddress2.setText("");
			}
		}
	}
	
	//****************************************************************************************************
	//
	//	setupGUI
	//		Creates the objects needed to run the GUI and sets up the GUI
	//
	public static void setupGUI () {
		
		//Create JPanel object
		JPanel panel = new JPanel();
		//Create JFrame object
		JFrame frame = new JFrame();
		
		//Setup frame
		frame.setSize(600, 230);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(panel);
		
		//Setup panel
		panel.setLayout(null);
		
		//Setup title box
		titleBox = new JLabel("----------YELP BUSINESS DATASET SIMILARITY SEARCH TOOL----------");
		titleBox.setBounds(60, 5, 500, 25);
		panel.add(titleBox);
		
		//Setup search label
		searchLabel = new JLabel("Buisiness Name :");
		searchLabel.setBounds(20, 40, 120, 25);
		panel.add(searchLabel);
		
		//Setup search text box
		searchText = new JTextField();
		searchText.setBounds(130, 40, 300, 25);
		panel.add(searchText);
		
		//Setup search button
		button = new JButton("Search");
		button.setBounds(440, 40, 80, 25);
		button.addActionListener(new YelpDatasetSimilarBusinesses());
		panel.add(button);
		
		// Setup first response label set
		responseName = new JLabel("");
		responseName.setBounds(130, 70, 500, 25);
		panel.add(responseName);
		responseAddress = new JLabel("");
		responseAddress.setBounds(130, 90, 500, 25);
		panel.add(responseAddress);
		
		// Setup second response label set
		responseName2 = new JLabel("");
		responseName2.setBounds(130, 120, 500, 25);
		panel.add(responseName2);
		responseAddress2 = new JLabel("");
		responseAddress2.setBounds(130, 140, 500, 25);
		panel.add(responseAddress2);
		
		//Set frame to be visible and in focus
		frame.setVisible(true);
	}
	
	//****************************************************************************************************
	//
	//	jsonToObjAry
	//		Converts first x lines of JSON file into business class objects (where x is the
	//		set class constant MAX_NUM) and inserts the objects into the array of business
	//		objects called businesses.
	//
	public static void jsonToObjAry() {
		
		businesses = new Business[MAX_NUM];		// Array of business objects limited to MAX_NUM class constant
		
		try (BufferedReader reader = new BufferedReader(new FileReader(fileAddress))) {
			 
			 // Class Variables
			 String line;						// Line of JSON file read in through buffered reader as a string
			 int index = 0;						// Index for adding to array of businesses
			 
			 // While line is not null
			 while ((line = reader.readLine()) != null) {		 
				 // Create new business object from JSON file line taking in business object variables
				 Business business = new Gson().fromJson(line, Business.class);
				 // Insert business information into object array of businesses
				 businesses[index] = business;				 
				 // Increment index counter
				 index++;
			 }
	    } catch(IOException e) {
	    	e.printStackTrace();
	    }
	}
	
	//****************************************************************************************************
	//
	//	populateHashmaps 
	//		Inserts the business id, name, and categories into hash maps and creates two arrays
	//		containing the entire set of all categories listed (including repeats) and an array
	//		of arrays containing the categories of each business.
	//
	public static void populateHashmaps() {
		
		String[] categories = new String[MAX_SEARCH_CATEGORIES];			// Array initialized to hold categories of a single business
		
		// Test - Indicate start of hashmap population //==================================================================================================SYSTEM OUTPUT
		System.out.println("Populating Hashmaps...");
		
		// For each business within the array of businesses
		for (int x = 0; x < businesses.length; x++) {
			// If business categories are not null
			if (businesses[x].categories != null) {
				// Create array of categories
				categories = businesses[x].categories.split(", ");
				// For each category within the array of categories
				for (int c = 0; c < categories.length; c++) {
					// Add to array containing all of categories of all businesses
					catAryList.add(categories[c]);
					// Add to category word frequency table
					WordFrequencyTable.add(categories[c]);
				}
				// Add to array containing all arrays of categories of all businesses
				catAryAryList.add(categories);
			    // Put values into hashmaps
				bName_bId.put(businesses[x].name, businesses[x].business_id); 						// Hashmap of name->id
				bId_bName.put(businesses[x].business_id, businesses[x].name); 						// Hashmap of id->name
			    bId_category.put(businesses[x].business_id, businesses[x].categories.split(", ")); 	// Hashmap of id->categories
			    index_bId.put(x, businesses[x].business_id); 										// Hashmap of index->id (helps with accessing business_id from index)
			    bId_index.put(businesses[x].business_id, x); 										// Hashmap of id->index (helps with accessing catAryAryList)
			    bId_address.put(businesses[x].business_id, businesses[x].address);					// Hashmap of id->address
			}
		}
		System.out.print("Calculating TFIDFs...");
		// Traverse through populated businesses array to get TFIDF of each category
		for(int count = 0; count < businesses.length; count++) {
			
			// Test - Show percentage calculated	//====================================================================================================SYSTEM OUTPUT
			///*
			if (count == (0.1 * businesses.length))
				System.out.print("10%...");
			else if (count == (0.2 * businesses.length))
				System.out.print("20%...");
			else if (count == (0.3 * businesses.length))
				System.out.print("30%...");
			else if (count == (0.4 * businesses.length))
				System.out.print("40%...");
			else if (count == (0.5 * businesses.length))
				System.out.print("50%...");
			else if (count == (0.6 * businesses.length))
				System.out.print("60%...");
			else if (count == (0.7 * businesses.length))
				System.out.print("70%...");
			else if (count == (0.8 * businesses.length))
				System.out.print("80%...");
			else if (count == (0.9 * businesses.length))
				System.out.print("90%...");
			//*/
			
			// If business categories are not null
			if (businesses[count].categories != null) {
				// Create array of categories
				categories = businesses[count].categories.split(", ");
				// For each category within the array of categories
				for (int c = 0; c < categories.length; c++) {
					// Calculate tfidf and apply to category_tfidf hashmap
					category_tfidf.put(categories[c], tfIdf(catAryList, catAryAryList, categories[c]));			
				}
			}
		}
		// Test - Show percentage calculated / indicate end of hashmap population	//=====================================================================SYSTEM OUTPUT
		System.out.println("100%");
		System.out.println("TFIDFs Calculated.");
		System.out.println("Hashmaps Complete");
	}
	
	//****************************************************************************************************
	//
	//	Tf (Term Frequency)
	//		Calculates the Term Frequency of the given term within the arrays created by the 
	//		populateHashmaps method.
	//		TF = Number of times term appears in document (business categories)
	//
	public static double tf(ArrayList<String> doc, String term) {
        double result = 0;
        for (String word : doc) {
            if (term.equalsIgnoreCase(word))
                result++;
        }
        return result / doc.size();
    }
	
	//****************************************************************************************************
	//
	//	Idf (Inverse Document Frequency)
	//		Calculates the Inverse Document Frequency of the given term using the word frequency table.
	//		IDF = log((1 + total number of documents(businesses)/1 + number of documents with term) + 1)
	//
	public static double idf(ArrayList<String[]> docs, String term) {
        double n = 0;
        n = WordFrequencyTable.getCount(term);				// Implementation of word frequency table
        return Math.log(((1 + docs.size()) /(1 + n)) + 1);
    }
	
	//****************************************************************************************************
	//
	//	tfIdf
	//		Calculates the Term Frequency Inverse Document Frequency by sending the given parameters
	//		of an array list of strings (doc), an array list of string arrays (docs),
	//		and a string (term) through TF and IDF and then calculating the product.
	//
	public static double tfIdf(ArrayList<String> doc, ArrayList<String[]> docs, String term) {
        return tf(doc, term) * idf(docs, term);
    }
	
	//****************************************************************************************************
	//
	// cosineSimilarity
	// 		Calculates cosine similarity between the 2 docs (list of strings containing categories) using
	//		business categories -> TFIDF hash map.
	//	
	public static double cosineSimilarity(String[] doc1, String[] doc2, Map<String, Double> tfIdfMap) {
		// Create a set of all unique words from both documents
		Set<String> uniqueWords = new HashSet<>();
		uniqueWords.addAll(Arrays.asList(doc1));
		uniqueWords.addAll(Arrays.asList(doc2));

		// Calculate dot product and magnitude of vectors for cosine similarity
		double dotProduct = 0;
		double doc1Magnitude = 0;
		double doc2Magnitude = 0;
		// For each word within set of unique words
		for (String word : uniqueWords) {
			// Get TFIDF of word within doc1(business 1 categories) and doc2 (business 2 categories)
			double tfIdf1 = tfIdfMap.getOrDefault(word, 0.0) * countOccurrences(word, doc1);
			double tfIdf2 = tfIdfMap.getOrDefault(word, 0.0) * countOccurrences(word, doc2);
			// Calculate dot product
			dotProduct += tfIdf1 * tfIdf2;
			// Calculate magnitude of vectors
			doc1Magnitude += tfIdf1 * tfIdf1;
			doc2Magnitude += tfIdf2 * tfIdf2;
		}

		// Calculate cosine similarity
		double similarity = dotProduct / (Math.sqrt(doc1Magnitude) * Math.sqrt(doc2Magnitude));
		
		// Return similarity
		return similarity;
	}

	//****************************************************************************************************
	//
	// countOccurrences
	// 		Helper function to count the number of occurrences of a word in a document.
	//	
	private static int countOccurrences(String term, String[] document) {
		int count = 0;
		for (String word : document) {
			if (term.equalsIgnoreCase(word)) {
				count++;
				break;
			}
		}
		return count;
	}

	//****************************************************************************************************
	//
	// validName
	//		Helper function to search through a given array, returning true if the given name is found.
	//	
	private static boolean validName(String name, Business[] businesses){
		int i = 0;
		for(i = 0; i < businesses.length; i++) {
			if(businesses[i].name.equals(name)) {
				return true;
			}
		}
		return false;
	}

	//****************************************************************************************************
	//
	// similarBusinesses
	//		Similarity metric used to correlate similarity between a given array of category strings of
	//		a single business and the categories of the rest of the list of businesses using the 
	//		hashmaps between business id -> business categories and business categories -> TFIDF,
	//		before updating the array to remove the most similar business (the searched business
	//		itself) and return the resulting two most similar businesses to the searched business.
	//	
	private static String[] similarBusinesses(String[] category, Map<String, String[]> bId_category, Map<String, Double> tfIdfMap, String searchId) {
		String[] bizIds = new String[NUM_SIMILAR_BUSINESSES]; 					// contains business IDs of 3 businesses with highest similarity metrics
		double[] similarityMetric = new double[NUM_SIMILAR_BUSINESSES]; 		// contains similarity metric values of 3 businesses with highest similarity metrics
		
		// For each entry of the bId_category hashmap
		for (Map.Entry<String, String[]> set : bId_category.entrySet()) {
			// If searched business id is not equal to the entry of the bId_category hashmap
			if(searchId != set.getKey()) {
				// Get cosine similarity
				double x = cosineSimilarity(category, set.getValue(), tfIdfMap);
				// If cosine similarity of entry is greater than any of the stored values within the array
				if(x > similarityMetric[0] || x > similarityMetric[1]){
					// Replace the lowest similarity metric value
					int lowest_index = getSmallestValueIndex(similarityMetric);
					similarityMetric[lowest_index] = x;
					bizIds[lowest_index] = set.getKey();
				}
			}
		}
		// Update bizIds
		int[] arr = returnSortArrayIndex(similarityMetric);
		
		// 2nd and 3rd most similar bizIds will be stored as most similar will be the same business itself
		String[] upBizIds = new String[NUM_SIMILAR_BUSINESSES];
		for(int count = 0; count < NUM_SIMILAR_BUSINESSES; count++) {
			upBizIds[count] = bizIds[arr[count]];
		}
		
		/*
		System.out.println("Searched id : " + searchId);
		System.out.println("Full Array : " + Arrays.toString(bizIds));
		System.out.println("Before swap : " + Arrays.toString(upBizIds));
		*/
		
		// Swap bizIds so that it is in descending order
		int updateCount = 0;
		for(int count = bizIds.length - 1; count >= 0; count--) {
			upBizIds[updateCount] = bizIds[count];
			updateCount++;
		}
		
		//System.out.println("After swap : " + Arrays.toString(upBizIds));
		
		// Return businessIds
		return upBizIds;
	}
	
	//****************************************************************************************************
	//
	// returnSortArrayIndex
	//		Searches through given array returning a sorted array.
	//
	private static int[] returnSortArrayIndex(double[] inputArr) {
		int[] arr = new int[inputArr.length];
		for(int i = 0; i<inputArr.length; i++){
			arr[i] = i;
		}
		// Bubble Sort implementation
		for(int i=0; i<inputArr.length; i++) {
			for(int j=0; j<inputArr.length - i - 1; j++) {
				if(inputArr[j] > inputArr[j+1]) {
					// swap array values
					double temp = inputArr[j];
                    inputArr[j] = inputArr[j + 1];
                    inputArr[j + 1] = temp;

					// swap indices values
					int t = arr[j];
					arr[j] = arr[j + 1];
                    arr[j + 1] = t;
				}
			}
		}
		return arr;
	}
	
	//****************************************************************************************************
	//
	// getSmallestValueIndex
	//		Searches through given array returning the index of the lowest value.
	//	
	private static int getSmallestValueIndex(double[] arr) {
		int lowest_value = 0;
		for(int i = 0; i < arr.length; i++) {
			if(arr[i] < arr[lowest_value]) {
				lowest_value = i;
			}
		}
		return lowest_value;
	}
	
	//****************************************************************************************************
	//
	// searchBusinesses
	//		Searches through businesses array using the given businessName and the similarBusinesses
	//		similarity metric to get the three most similar businesses IDs before converting them to
	//		the corresponding business names and returning an array of the names of the most similar
	//		businesses.
	//	
	public static String[] searchBusinesses(String businessName) {
		
		// Instantiate string arrays for search and result
		String[] searchCategories = new String[MAX_SEARCH_CATEGORIES];
		String[] resultIds = new String[NUM_SIMILAR_BUSINESSES];
		String[] resultNames = new String[NUM_SIMILAR_BUSINESSES];
		
		// Get search business id
		String searchId = bName_bId.get(businessName);
		
		// Get string array of categories from given business name to search
		searchCategories = bId_category.get(searchId);
		
		// Search using similarBusinesses
		resultIds = similarBusinesses(searchCategories, bId_category, category_tfidf, searchId);
		
		// For each resulting similar business id
		for(int count = 0; count < resultIds.length; count++) {
			// Convert business id to business name
			resultNames[count] = bId_bName.get(resultIds[count]);	
		}
		// Return string array containing resulting similar business names
		return resultNames;
	}
	
	//****************************************************************************************************
	//
	// getSimilarIds
	//		Searches through businesses array using the given businessName and the similarBusinesses
	//		similarity metric to get and return the three most similar businesses IDs.
	//		
	public static String[] getSimilarIds(String businessName) {
		
		//Get categories of given search term
		String[] searchCategories = new String[MAX_SEARCH_CATEGORIES];
		String[] resultIds = new String[NUM_SIMILAR_BUSINESSES];
		
		// Get search business id
		String searchId = bName_bId.get(businessName);
		
		// Get string array of categories from given business name to search
		searchCategories = bId_category.get(bName_bId.get(businessName));
		
		// Search using similarBusinesses
		resultIds = similarBusinesses(searchCategories, bId_category, category_tfidf, searchId);
		
		// Return string array containing resulting similar business names
		return resultIds;
	}
	
	//****************************************************************************************************
	//
	// getSimilarAddresses
	//		Searches through businesses array using the given businessName and the similarBusinesses
	//		similarity metric to get and return the three most similar businesses IDs.
	//		
	public static String[] getSimilarAddresses(String businessName) {
		
		//Get categories of given search term
		String[] searchCategories = new String[MAX_SEARCH_CATEGORIES];
		String[] resultAddresses = new String[NUM_SIMILAR_BUSINESSES];
		
		// Get search business id
		String searchId = bName_bId.get(businessName);
		
		// Get string array of categories from given business name to search
		searchCategories = bId_category.get(bName_bId.get(businessName));
		
		// Search using similarBusinesses
		resultAddresses = similarBusinesses(searchCategories, bId_category, category_tfidf, searchId);
		
		// For each resulting similar business id
		for(int count = 0; count < resultAddresses.length; count++) {
			// Convert business id to business name
			resultAddresses[count] = bId_address.get(resultAddresses[count]);	
		}
		// Return string array containing resulting similar business names
		return resultAddresses;
	}
}