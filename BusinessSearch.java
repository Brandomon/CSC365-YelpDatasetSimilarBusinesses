//*************************************************************************************************
//
//	Brandon LaPointe & Param Rajguru
//	CSC365 - Professor Doug Lea
//	BusinessSearch.java
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
import com.google.gson.Gson;	// Uses GSON to parse JSON

public class BusinessSearch implements ActionListener {
	
	// Class Constants & Variables
	private static final int MAX_NUM = 10_000;														// Maximum number of businesses to add to object array
	private static String fileAddress = "yelp_dataset_business.json";								// Address of JSON file used (Yelp Academic Dataset Business)
	private static Business[] businesses;															// Array of business objects
	private static ArrayList<String> catAryList = new ArrayList<String>();							// Expandable array list containing all of the categories from all businesses
	private static ArrayList<String[]> catAryAryList = new ArrayList<String[]>();					// Array list containing string arrays of categories from all businesses
	private static String[] searchResultNames = new String[3];										// Array containing the search result of the 3 most similar business names
	private static String[] searchResultAddresses = new String[3];									// Array containing the search result of the 3 most similar business addresses
	
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
	private static JLabel responseName;			// Result 1 name of similarity search
	private static JLabel responseName2;		// Result 2 name of similarity search
	private static JLabel responseName3;		// Result 3 name of similarity search
	private static JLabel responseAddress;		// Result 1 address of similarity search
	private static JLabel responseAddress2;		// Result 2 address of similarity search
	private static JLabel responseAddress3;		// Result 3 address of similarity search
	
	//*************************************************************************************************
	//
	// Business Object Class
	//
	public class Business {		
		// Object Variables
		private String business_id;
		private String name;
		private String address;
		private String categories;
		
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
	
	//*************************************************************************************************
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
		
		System.out.println("Populating Hashmaps...");
		
		// Populate hashmaps using businesses object array
		populateHashmaps();
		
		System.out.println("Hashmaps complete");
		
	}
	
	//*************************************************************************************************
	//
	//	actionPerformed
	//		Code run when search button is pressed to search for similar businesses.
	//
	@Override
	public void actionPerformed(ActionEvent e) {
		
		// Convert searchText box input to string
		String search = searchText.getText();
		responseName.setText("");
		responseName2.setText("");
		responseName3.setText("");
		responseAddress.setText("");
		responseAddress2.setText("");
		responseAddress3.setText("");
		
		// If search field is empty
		if (search.equalsIgnoreCase("")) {
			responseName.setText("ERROR - Empty Search Text Field");
			responseName2.setText("");
			responseName3.setText("");
			responseAddress.setText("");
			responseAddress2.setText("");
			responseAddress3.setText("");
		}
		// Else if search field contains anything
		else if (!search.equalsIgnoreCase("")) {
			// If searched business name is found within businesses array
			if (validName(search, businesses) == true) {
				// Search for similar businesses
				searchResultNames = searchBusinesses(search);
				searchResultAddresses = getSimilarAddresses(search);
				
				// Display the results on GUI
				responseName.setText("Name : " + searchResultNames[0]);
				responseName2.setText("Name : " + searchResultNames[1]);
				responseName3.setText("Name : " + searchResultNames[2]);
				responseAddress.setText("Address : " + searchResultAddresses[0]);
				responseAddress2.setText("Address : " + searchResultAddresses[1]);
				responseAddress3.setText("Address : " + searchResultAddresses[2]);
			}
			else {
				// Display error on GUI
				responseName.setText("ERROR - Business not found within dataset");
				responseName2.setText("");
				responseName3.setText("");
				responseAddress.setText("");
				responseAddress2.setText("");
				responseAddress3.setText("");
			}
		}
	}
	
	//*************************************************************************************************
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
		frame.setSize(600, 270);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(panel);
		
		//Setup panel
		panel.setLayout(null);
		
		//Setup title box
		titleBox = new JLabel("----------YELP BUSINESS DATASET SIMILARITY SEARCH TOOL----------");
		titleBox.setBounds(90, 5, 400, 25);
		panel.add(titleBox);
		
		//Setup search label
		searchLabel = new JLabel("Buisiness Name :");
		searchLabel.setBounds(20, 40, 120, 25);
		panel.add(searchLabel);
		
		//Setup search text box
		searchText = new JTextField();
		searchText.setBounds(130, 40, 300, 25);
		panel.add(searchText);
		
		//Setup login button
		button = new JButton("Search");
		button.setBounds(440, 40, 80, 25);
		button.addActionListener(new BusinessSearch());
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
		
		// Setup third response label set
		responseName3 = new JLabel("");
		responseName3.setBounds(130, 170, 500, 25);
		panel.add(responseName3);
		responseAddress3 = new JLabel("");
		responseAddress3.setBounds(130, 190, 500, 25);
		panel.add(responseAddress3);
		
		//Set frame to be visible and in focus
		frame.setVisible(true);
	}
	
	//*************************************************************************************************
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
				 // Create new business object from JSON file line
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
	
	//*************************************************************************************************
	//
	//	populateHashmaps 
	//		Inserts the business id, name, and categories into hash maps and creates two arrays
	//		containing the entire set of all categories listed (including repeats) and an array
	//		of arrays containing the categories of each business.
	//
	public static void populateHashmaps() {
		
		String[] categories = new String[30];			// Array initialized to hold categories of a single business
		
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
		// Traverse through populated businesses array to get TFIDF of each category
		for(int count = 0; count < businesses.length; count++) {
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
	}
	
	//*************************************************************************************************
	//
	//	Tf
	//		Calculates the Term Frequency of the given term within the arrays created by the 
	//		populateHashmaps method.
	//
	public static double tf(ArrayList<String> doc, String term) {
        double result = 0;
        for (String word : doc) {
            if (term.equalsIgnoreCase(word))
                result++;
        }
        return result / doc.size();
    }
	
	//*************************************************************************************************
	//
	//	Idf 
	//		Calculates the Inverse Document Frequency of the given term within the array of arrays
	//		containing the business categories of each business object.
	//
	public static double idf(ArrayList<String[]> docs, String term) {
        double n = 0;
        for (String[] doc : docs) {
            for (String word : doc) {
                if (term.equalsIgnoreCase(word)) {
                    n++;
                    break;
                }
            }
        }
        // Print number of occurrences of given term
        //System.out.println("Occurrences of n : " + n);
        return Math.log(docs.size() / n);
    }
	
	//*************************************************************************************************
	//
	//	tfIdf
	//		Calculates the Term Frequency Inverse Document Frequency by sending the given parameters
	//		of an array list of strings (doc), an array list of string arrays (docs),
	//		and a string (term) through TF and IDF and then calculating the product.
	//
	public static double tfIdf(ArrayList<String> doc, ArrayList<String[]> docs, String term) {
        return tf(doc, term) * idf(docs, term);
    }
	
	// *************************************************************************************************
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
			// Get TFIDF of word within doc1 and doc2
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

	// *************************************************************************************************
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

	//*************************************************************************************************
	//
	// validName
	//		Searches through given array returning true if name is found.
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

	//*************************************************************************************************
	//
	// similarBusinesses
	//		Similarity metric used to correlate similarity between a given array of category strings of
	//		a single business and the categories of the rest of the list of businesses using the 
	//		hashmaps between business id -> business categories and business categories -> TFIDF,
	//		returning an array containing the three most similar businesses.
	//	
	private static String[] similarBusinesses(String[] category, Map<String, String[]> bId_category, Map<String, Double> tfIdfMap) {
		String[] bizIds = { "", "", "" }; 			// contains business ids of businesses with highest similarity metrics
		double[] similarityMetric = { 0, 0, 0 }; 	// contains values of highest similarity metrics
		
		// For each entry of the bId_category hashmap
		for (Map.Entry<String, String[]> set : bId_category.entrySet()) {
			//
			double x = cosineSimilarity(category, set.getValue(), tfIdfMap);
			//
			if(x > similarityMetric[0] || x > similarityMetric[1] || x > similarityMetric[2]){
				int lowest_index = getSmallestIndex(similarityMetric);
				similarityMetric[lowest_index] = x;
				bizIds[lowest_index] = set.getKey();
			}
		}
		return bizIds;
	}
	
	//*************************************************************************************************
	//
	// getSmallestIndex
	//		Searches through given array returning the lowest index value.
	//	
	private static int getSmallestIndex(double[] arr){
		int lowest_index = 0;
		for(int i = 0; i < arr.length; i++) {
			if(arr[i] < arr[lowest_index]) {
				lowest_index = i;
			}
		}
		return lowest_index;
	}
	
	//*************************************************************************************************
	//
	// searchBusinesses
	//		Searches through businesses array using the given businessName and the similarBusinesses
	//		similarity metric to get the three most similar businesses IDs before converting them to
	//		the corresponding business names and returning an array of the names of the most similar
	//		businesses.
	//	
	public static String[] searchBusinesses(String businessName) {
		
		//Get categories of given search term
		String[] searchCategories = new String[30];
		String[] resultIds = new String[3];
		String[] resultNames = new String[3];
		
		// Get string array of categories from given business name to search
		searchCategories = bId_category.get(bName_bId.get(businessName));
		
		// Search using similarBusinesses
		resultIds = similarBusinesses(searchCategories, bId_category, category_tfidf);
		
		// For each resulting similar business id
		for(int count = 0; count < 3; count++) {
			// Convert business id to business name
			resultNames[count] = bId_bName.get(resultIds[count]);	
		}
		// Return string array containing resulting similar business names
		return resultNames;
	}
	
	//*************************************************************************************************
	//
	// getSimilarIds
	//		Searches through businesses array using the given businessName and the similarBusinesses
	//		similarity metric to get and return the three most similar businesses IDs.
	//		
	public static String[] getSimilarIds(String businessName) {
		
		//Get categories of given search term
		String[] searchCategories = new String[30];
		String[] resultIds = new String[3];
		
		// Get string array of categories from given business name to search
		searchCategories = bId_category.get(bName_bId.get(businessName));
		
		// Search using similarBusinesses
		resultIds = similarBusinesses(searchCategories, bId_category, category_tfidf);
		
		// Return string array containing resulting similar business names
		return resultIds;
	}
	
	//*************************************************************************************************
	//
	// getSimilarAddresses
	//		Searches through businesses array using the given businessName and the similarBusinesses
	//		similarity metric to get and return the three most similar businesses IDs.
	//		
	public static String[] getSimilarAddresses(String businessName) {
		
		//Get categories of given search term
		String[] searchCategories = new String[30];
		String[] resultAddresses = new String[3];
		
		// Get string array of categories from given business name to search
		searchCategories = bId_category.get(bName_bId.get(businessName));
		
		// Search using similarBusinesses
		resultAddresses = similarBusinesses(searchCategories, bId_category, category_tfidf);
		
		// For each resulting similar business id
		for(int count = 0; count < 3; count++) {
			// Convert business id to business name
			resultAddresses[count] = bId_address.get(resultAddresses[count]);	
		}
		// Return string array containing resulting similar business names
		return resultAddresses;
	}
}