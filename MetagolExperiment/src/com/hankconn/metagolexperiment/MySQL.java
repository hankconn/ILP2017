package com.hankconn.metagolexperiment;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQL {
	static Connection connect;
	
	public static void init() throws Exception
	{
        Class.forName("com.mysql.jdbc.Driver");
        connect = DriverManager
                .getConnection("jdbc:mysql://localhost:3306/metagoldb?user=root");

        PreparedStatement preparedStatement = connect
                .prepareStatement("SELECT ID, username, password from users");
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {}
	}
	
	public static void saveResult(int increment, int trialNum, int numIndividuals, 
			String predicate, int trainingSize, int testSize, String definitionLearned,
			int[] result, int experimentID, long runningTime) throws SQLException
	{
		String sql = "INSERT INTO trialResults (";
		sql += "randomOrderSwaps,trialNumber,numIndividuals,";
		sql += "predicate,trainingSize,testSize,definitionLearned,";
		sql += "totalPositives,truePositives,falsePositives,totalNegatives,trueNegatives,falseNegatives, experimentID,";
		sql += "runningTime) VALUES (?,?,?,"
				+ "?,?,?,?,"
				+ "?,?,?,?,?,?,?,?)";
		
        PreparedStatement preparedStatement = connect
                .prepareStatement(sql);
        
        preparedStatement.setInt(1, increment);
        preparedStatement.setInt(2, trialNum);
        preparedStatement.setInt(3, numIndividuals);
        
        preparedStatement.setString(4, predicate);
        preparedStatement.setInt(5, trainingSize);
        preparedStatement.setInt(6, testSize);
        preparedStatement.setString(7, definitionLearned);
        
        preparedStatement.setInt(8, result[0]);
        preparedStatement.setInt(9, result[1]);
        preparedStatement.setInt(10, result[2]);
        preparedStatement.setInt(11, result[3]);
        preparedStatement.setInt(12, result[4]);
        preparedStatement.setInt(13, result[5]);
        preparedStatement.setInt(14, experimentID);
        preparedStatement.setInt(15, (int)runningTime);
        
        preparedStatement.executeUpdate();
	}
}
