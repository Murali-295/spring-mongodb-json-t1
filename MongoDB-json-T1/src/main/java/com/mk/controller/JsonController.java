package com.mk.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.bson.Document;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RestController;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class JsonController {

	@Autowired
	private ResourceLoader resourceLoader;
	private MongoClient mongoClient = MongoClients.create();
	private MongoDatabase mongoDatabase;

	@GetMapping("/get")
	public String getJson() throws IOException {

		Resource resource = resourceLoader.getResource("classpath:sample.json");

		InputStream inputStream = resource.getInputStream();
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		String copyToString = FileCopyUtils.copyToString(inputStreamReader);

		// converting string to jsonObject
		JSONObject jsonObject = new JSONObject(copyToString);

		// creating database by parsing the data
		for (String databaseName : jsonObject.keySet()) {
			mongoDatabase = mongoClient.getDatabase(databaseName);
			System.out.println(databaseName + " database is created");

			JSONObject jsonObject2 = jsonObject.getJSONObject(databaseName);

			// creating & adding values to the collection
			for (String collectionName : jsonObject2.keySet()) {
				// creating a collection for inserting the values into it
				MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);

				// json-array to store the values and iterate over them
				JSONArray jsonArray = jsonObject2.getJSONArray(collectionName);

				for (int i = 0; i < jsonArray.length(); i++) {
					// storing each array by converting from array to an object
					JSONObject arrayValues = jsonArray.getJSONObject(i);

					// document to store the data in key value pairs such that data can be directly
					// added to database
					Document document = new Document(arrayValues.toMap());

					// inserting single row data
					collection.insertOne(document);
				}
			}
		}

		return "Json data retrieved";
	}

	@GetMapping("/returnData")
	public Map<String, Object> returnData() {
		mongoDatabase = mongoClient.getDatabase("components");

		// map to store the database name and its respective values
		Map<String, Object> totalData = new HashMap<>();
		// map to store table name and the map values
		Map<String, Object> tableData = new HashMap<>();

		// iterating over database to get table and its data
		for (String name : mongoDatabase.listCollectionNames()) {
			// to store column values
			ArrayList<Document> tableValues = new ArrayList<>();

			// to get collections data
			MongoCollection<Document> keyValues = mongoDatabase.getCollection(name);

			// iterating over the table data & storing data in the form of document
			for (Document document : keyValues.find()) {
				// remove "_id" key for every value
				document.remove("_id");
				tableValues.add(document);
			}

			// storing the table name and its data
			tableData.put(name, tableValues);
		}
		// storing the database name and all the tables data
		totalData.put("components", tableData);

		return totalData;
	}
}
