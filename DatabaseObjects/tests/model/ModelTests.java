package model;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Random;

import model.Filter.FilterType;
import model.Filter.RelationType;

public class ModelTests
{
	public static final Filter FILTER_NAME;
	public static final Filter FILTER_ITEMS_BOUGHT;
	
	static
	{
		var temp = new Filter();
		
		temp.filterColumn(CustomerTable.NAME.cloneWithValue("pickles"), FilterType.EQUAL);
		FILTER_NAME = temp;
		
		temp = new Filter();
		
		temp.filterColumn(CustomerTable.ITEMS_BOUGHT.cloneWithValue(60), FilterType.GREATER_THAN_EQUAL);
		temp.filterColumn(RelationType.OR, CustomerTable.ITEMS_BOUGHT.cloneWithValue(30), FilterType.LESS_THAN_EQUAL);
		FILTER_ITEMS_BOUGHT = temp;
	}
	
	public static void main(String[] args) throws SQLException, ClassNotFoundException
	{
		var random = new Random();
		var customer = new CustomerTable();
		customer.getColumn(CustomerTable.ITEMS_BOUGHT).setValue(random.nextInt(101));
		customer.getColumn(CustomerTable.NAME).setValue("Test");
		
		Database.registerDriver("com.mysql.jdbc.Driver");
		Database db = new Database("jdbc:mysql://127.0.0.1:3306/gameserver", "root", "minecraft");
		
		customer.commit(db);
		
		Join join = new Join(new CustomerTable());
		join.joinOnValue(new Filter().filterColumn(CustomerTable.ID, FilterType.EQUAL));
		join.joinOnValue(new Filter().filterColumn(CustomerTable.NAME, FilterType.NOT_EQUAL));
		join.joinOnColumn(RelationType.OR, new Filter().filterColumn(CustomerTable.ITEMS_BOUGHT, FilterType.GREATER_THAN), new ProductTable(), ProductTable.PRODUCT_ID);
		System.out.println(join);
		
		System.out.println();
		
		Query q = Query.query(db, ProductTable.class)
					   .filter(ProductTable.PRODUCT_ID, 43)
					   .join(join);
		
		System.out.println(q);
	}
	
	private static class CustomerTable extends Table
	{
		public static final Column<Integer> ID =
				new Column<Integer>(Integer.class, "id", Types.INTEGER, 0, true, false, true, null);
		
		public static final Column<String> NAME =
				new Column<String>(String.class, "name", Column.TEXT, 0, false, false, false, null);
		
		public static final Column<Integer> ITEMS_BOUGHT =
				new Column<Integer>(Integer.class, "itemsbought", Types.INTEGER, 0, false, false, false, null);
		
		public CustomerTable()
		{
			super("customer", ID.typeClone(), NAME.typeClone(), ITEMS_BOUGHT.typeClone());
		}
	}
	
	private static class ProductTable extends Table
	{
		public static final Column<Integer> PRODUCT_ID = ColumnBuilder.start(Integer.class, Types.INTEGER)
																	  .setName("productid")
																	  .build();
		
		public ProductTable()
		{
			super("product", PRODUCT_ID.typeClone());
		}
	}
}
