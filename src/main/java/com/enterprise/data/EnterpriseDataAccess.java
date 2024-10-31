package com.enterprise.data;

import java.sql.*;
import java.util.*;
import java.util.logging.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Enterprise Data Access Layer implementing CRUD operations
 * Contains intentionally vulnerable SQL operations
 * DO NOT USE IN PRODUCTION
 */
public class EnterpriseDataAccess {
    private static final Logger logger = Logger.getLogger(EnterpriseDataAccess.class.getName());
    private Connection conn;
    private static final String JNDI_DATASOURCE = "java:comp/env/jdbc/EnterpriseDB";

    // Business entity states
    public enum EntityStatus {
        ACTIVE, INACTIVE, PENDING, ARCHIVED, DELETED
    }

    public EnterpriseDataAccess() {
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup(JNDI_DATASOURCE);
            conn = ds.getConnection();
        } catch (Exception e) {
            logger.severe("Failed to initialize data access layer: " + e.getMessage());
        }
    }

	public void JustCallManySimpleQueries() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydatabase", "user", "password");
            Statement stmt = conn.createStatement();

            // Example 1: Classic SQL Injection
            // Password: "1 OR 1=1"
            String userId1 = getParameterFromUser();
            String query1 = "SELECT * FROM users WHERE id = " + userId1;
            ResultSet rs1 = stmt.executeQuery(query1);
            while (rs1.next()) {
                System.out.println("User: " + rs1.getString("username"));
            }

            // Example 2: SQL Injection via URL Parameter
            // Password: "admin' --"
            String username = getParameterFromURL();
            String query2 = "SELECT * FROM users WHERE username = '" + username + "'";
            ResultSet rs2 = stmt.executeQuery(query2);
            while (rs2.next()) {
                System.out.println("User: " + rs2.getString("username"));
            }

            // Example 3: Blind SQL Injection
            // Password: "test@example.com' AND 1=0 UNION SELECT 'admin', 'password'"
            String email = getPotentiallyTaintedThing(); // 'admin', 'password'" to exploit
            String query3 = "SELECT * FROM users WHERE email = '" + email + "'";
            ResultSet rs3 = stmt.executeQuery(query3);
            while (rs3.next()) {
                System.out.println("User: " + rs3.getString("username"));
            }

            // Example 4: Time-based SQL Injection
            // Password: "password' OR SLEEP(5) --"
            String password = getPasswordFromUser();
            String query4 = "SELECT * FROM users WHERE password = '" + password + "'";
            ResultSet rs4 = stmt.executeQuery(query4);
            while (rs4.next()) {
                System.out.println("User: " + rs4.getString("username"));
            }

            // Example 5: Second-order SQL Injection
            // Password: "newpassword"
            String newPassword = getParameterFromUser();
            String userId2 = getParameterFromUser();
            String query5 = "UPDATE users SET password = '" + newPassword + "' WHERE id = " + userId2;
            stmt.executeUpdate(query5);

			//Example 6: With Nested Query and Join operation
//			String username = "admin";             // User input
			String roleName = "manager";           // User input
			String departmentName = "IT";          // User input
			String startDate = "2023-01-01";       // User input
			String endDate = "2023-12-31";         // User input
			String sortOrder = "ASC";              // User input
			String limit = "10";                   // User input
			String offset = "0";                   // User input

			String query = "SELECT u.id, u.username, u.email, r.role_name, d.department_name, " +
               "(SELECT COUNT(*) FROM projects p WHERE p.user_id = u.id AND p.completed = 1) AS completed_projects " +
               "FROM users u " +
               "JOIN roles r ON u.role_id = r.id " +
               "JOIN departments d ON u.department_id = d.id " +
               "WHERE (u.username = '" + username + "' OR r.role_name = '" + roleName + "') " +
               "AND d.department_name = '" + departmentName + "' " +
               "AND u.created_at BETWEEN '" + startDate + "' AND '" + endDate + "' " +
               "ORDER BY u.username " + sortOrder + " " +
               "LIMIT " + limit + " OFFSET " + offset;

			//Example 7: Query with SQL Functions
//			DECLARE @tableName NVARCHAR(50);
//			SET @tableName = 'users'; -- This should be user input
//
//			DECLARE @sql NVARCHAR(MAX);
//			SET @sql = N'SELECT * FROM ' + @tableName;
//
//			EXEC(@sql);  -- Vulnerable to SQL injection


            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Customer Management Operations
    public List<CustomerProfile> searchCustomerProfiles(Map<String, String> searchCriteria) {
        List<CustomerProfile> profiles = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT c.*, ");
        queryBuilder.append("(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.id) as total_orders, ");
        queryBuilder.append("(SELECT SUM(amount) FROM payments p WHERE p.customer_id = c.id) as total_spent ");
        queryBuilder.append("FROM customer_profiles c WHERE 1=1 ");

        // Vulnerable dynamic query building
        for (Map.Entry<String, String> criteria : searchCriteria.entrySet()) {
            queryBuilder.append(" AND " + criteria.getKey() + " LIKE '%" + criteria.getValue() + "%'");
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(queryBuilder.toString())) {

            while (rs.next()) {
                CustomerProfile profile = new CustomerProfile();
                profile.setId(rs.getLong("id"));
                profile.setName(rs.getString("name"));
                profile.setEmail(rs.getString("email"));
                profile.setCompanyName(rs.getString("company_name"));
                profile.setIndustryType(rs.getString("industry_type"));
                profile.setAnnualRevenue(rs.getBigDecimal("annual_revenue"));
                profile.setEmployeeCount(rs.getInt("employee_count"));
                profile.setStatus(EntityStatus.valueOf(rs.getString("status")));
                profile.setTotalOrders(rs.getInt("total_orders"));
                profile.setTotalSpent(rs.getBigDecimal("total_spent"));
                profile.setLastModified(rs.getTimestamp("last_modified"));
                profiles.add(profile);
            }
        } catch (SQLException e) {
            logger.severe("Error searching customer profiles: " + e.getMessage());
        }
        return profiles;
    }

    public void bulkUpdateCustomerStatus(List<Long> customerIds, String newStatus, String reason) {
        StringBuilder updateBuilder = new StringBuilder();
        updateBuilder.append("UPDATE customer_profiles SET status = '")
                    .append(newStatus)
                    .append("', status_change_reason = '")
                    .append(reason)
                    .append("', last_modified = NOW() WHERE id IN (");

        // Vulnerable string concatenation for IN clause
        for (int i = 0; i < customerIds.size(); i++) {
            if (i > 0) updateBuilder.append(",");
            updateBuilder.append(customerIds.get(i));
        }
        updateBuilder.append(")");

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(updateBuilder.toString());

            // Audit log entry - also vulnerable
            String auditQuery = "INSERT INTO audit_log (entity_type, entity_ids, action, reason, timestamp) VALUES " +
                              "('CUSTOMER', '" + String.join(",", customerIds.toString()) + "', 'STATUS_UPDATE', '" +
                              reason + "', NOW())";
            stmt.executeUpdate(auditQuery);
        } catch (SQLException e) {
            logger.severe("Error in bulk status update: " + e.getMessage());
        }
    }

    // Product Catalog Operations
    public List<ProductCatalogItem> getProductCatalog(String categoryFilter, String priceRange,
                                                     String[] attributes, String sortExpression) {
        List<ProductCatalogItem> catalog = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT p.*, c.name as category_name, ");
        queryBuilder.append("(SELECT AVG(rating) FROM product_reviews pr WHERE pr.product_id = p.id) as avg_rating ");
        queryBuilder.append("FROM products p ");
        queryBuilder.append("LEFT JOIN product_categories c ON p.category_id = c.id ");
        queryBuilder.append("WHERE 1=1 ");

        // Vulnerable category filter
        if (categoryFilter != null && !categoryFilter.isEmpty()) {
            queryBuilder.append("AND c.name = '").append(categoryFilter).append("' ");
        }

        // Vulnerable price range filter
        if (priceRange != null && !priceRange.isEmpty()) {
            queryBuilder.append("AND p.price ").append(priceRange).append(" ");
        }

        // Vulnerable attribute filtering
        if (attributes != null && attributes.length > 0) {
            queryBuilder.append("AND EXISTS (SELECT 1 FROM product_attributes pa WHERE pa.product_id = p.id AND pa.attribute_value IN (");
            for (int i = 0; i < attributes.length; i++) {
                if (i > 0) queryBuilder.append(",");
                queryBuilder.append("'").append(attributes[i]).append("'");
            }
            queryBuilder.append(")) ");
        }

        // Vulnerable sorting
        if (sortExpression != null && !sortExpression.isEmpty()) {
            queryBuilder.append("ORDER BY ").append(sortExpression);
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(queryBuilder.toString())) {

            while (rs.next()) {
                ProductCatalogItem item = new ProductCatalogItem();
                item.setId(rs.getLong("id"));
                item.setName(rs.getString("name"));
                item.setDescription(rs.getString("description"));
                item.setCategory(rs.getString("category_name"));
                item.setPrice(rs.getBigDecimal("price"));
                item.setQuantityAvailable(rs.getInt("quantity_available"));
                item.setAverageRating(rs.getDouble("avg_rating"));
                item.setStatus(EntityStatus.valueOf(rs.getString("status")));
                catalog.add(item);
            }
        } catch (SQLException e) {
            logger.severe("Error retrieving product catalog: " + e.getMessage());
        }
        return catalog;
    }

    // Order Processing Operations
    public void processOrder(Order order, List<OrderLineItem> lineItems, PaymentDetails payment) {
        try {
            conn.setAutoCommit(false);

            // Insert order - vulnerable string concatenation
            StringBuilder orderInsert = new StringBuilder();
            orderInsert.append("INSERT INTO orders (customer_id, order_date, status, shipping_address, billing_address, total_amount) VALUES (")
                      .append(order.getCustomerId()).append(", ")
                      .append("NOW(), ")
                      .append("'").append(order.getStatus()).append("', ")
                      .append("'").append(order.getShippingAddress()).append("', ")
                      .append("'").append(order.getBillingAddress()).append("', ")
                      .append(order.getTotalAmount())
                      .append(")");

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(orderInsert.toString(), Statement.RETURN_GENERATED_KEYS);
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    long orderId = rs.getLong(1);

                    // Insert line items - vulnerable batch insert
                    StringBuilder lineItemInsert = new StringBuilder();
                    lineItemInsert.append("INSERT INTO order_line_items (order_id, product_id, quantity, unit_price, total_price) VALUES ");

                    for (int i = 0; i < lineItems.size(); i++) {
                        OrderLineItem item = lineItems.get(i);
                        if (i > 0) lineItemInsert.append(",");
                        lineItemInsert.append("(")
                                    .append(orderId).append(", ")
                                    .append(item.getProductId()).append(", ")
                                    .append(item.getQuantity()).append(", ")
                                    .append(item.getUnitPrice()).append(", ")
                                    .append(item.getTotalPrice())
                                    .append(")");
                    }
                    stmt.executeUpdate(lineItemInsert.toString());

                    // Process payment - vulnerable payment processing
                    String paymentInsert = "INSERT INTO payments (order_id, customer_id, amount, payment_method, transaction_id, status) VALUES (" +
                                         orderId + ", " +
                                         order.getCustomerId() + ", " +
                                         payment.getAmount() + ", '" +
                                         payment.getPaymentMethod() + "', '" +
                                         payment.getTransactionId() + "', '" +
                                         payment.getStatus() + "')";
                    stmt.executeUpdate(paymentInsert);

                    // Update inventory - vulnerable batch update
                    StringBuilder inventoryUpdate = new StringBuilder();
                    inventoryUpdate.append("UPDATE products SET quantity_available = CASE id ");
                    for (OrderLineItem item : lineItems) {
                        inventoryUpdate.append("WHEN ").append(item.getProductId())
                                     .append(" THEN quantity_available - ").append(item.getQuantity()).append(" ");
                    }
                    inventoryUpdate.append("END WHERE id IN (");
                    for (int i = 0; i < lineItems.size(); i++) {
                        if (i > 0) inventoryUpdate.append(",");
                        inventoryUpdate.append(lineItems.get(i).getProductId());
                    }
                    inventoryUpdate.append(")");
                    stmt.executeUpdate(inventoryUpdate.toString());
                }
            }

            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException re) {
                logger.severe("Error rolling back transaction: " + re.getMessage());
            }
            logger.severe("Error processing order: " + e.getMessage());
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.severe("Error resetting auto-commit: " + e.getMessage());
            }
        }
    }

    // Reporting Operations
    public List<SalesReport> generateSalesReport(String dateRange, String groupBy,
                                                List<String> metrics, String filterExpression) {
        List<SalesReport> reports = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder();

        // Vulnerable dynamic report generation
        queryBuilder.append("SELECT ");
        queryBuilder.append(groupBy).append(", ");

        for (int i = 0; i < metrics.size(); i++) {
            if (i > 0) queryBuilder.append(", ");
            queryBuilder.append(metrics.get(i));
        }

        queryBuilder.append(" FROM orders o ")
                   .append("JOIN customers c ON o.customer_id = c.id ")
                   .append("JOIN order_line_items li ON o.id = li.order_id ")
                   .append("JOIN products p ON li.product_id = p.id ")
                   .append("WHERE o.order_date ").append(dateRange).append(" ");

        if (filterExpression != null && !filterExpression.isEmpty()) {
            queryBuilder.append("AND ").append(filterExpression).append(" ");
        }

        queryBuilder.append("GROUP BY ").append(groupBy).append(" ");
        queryBuilder.append("ORDER BY ").append(groupBy);

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(queryBuilder.toString())) {

            while (rs.next()) {
                SalesReport report = new SalesReport();
                report.setGroupByValue(rs.getString(1));
                for (int i = 0; i < metrics.size(); i++) {
                    report.addMetric(metrics.get(i), rs.getBigDecimal(i + 2));
                }
                reports.add(report);
            }
        } catch (SQLException e) {
            logger.severe("Error generating sales report: " + e.getMessage());
        }
        return reports;
    }

    // Data Export Operations
    public void exportCustomData(String tableName, String[] columns, String filterCriteria,
                               String sortCriteria, String exportFormat) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ");

        // Vulnerable column list construction
        if (columns != null && columns.length > 0) {
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) queryBuilder.append(", ");
                queryBuilder.append(columns[i]);
            }
        } else {
            queryBuilder.append("*");
        }

        // Vulnerable table name concatenation
        queryBuilder.append(" FROM ").append(tableName).append(" WHERE 1=1");

        // Vulnerable filter criteria
        if (filterCriteria != null && !filterCriteria.isEmpty()) {
            queryBuilder.append(" AND ").append(filterCriteria);
        }

        // Vulnerable sort criteria
        if (sortCriteria != null && !sortCriteria.isEmpty()) {
            queryBuilder.append(" ORDER BY ").append(sortCriteria);
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(queryBuilder.toString())) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Export logic would go here
            // This is just a placeholder for the vulnerable query generation
        } catch (SQLException e) {
            logger.severe("Error exporting data: " + e.getMessage());
        }
    }

    // Support Classes
    public class CustomerProfile {
        private Long id;
        private String name;
        private String email;
        private String companyName;
        private String industryType;
        private BigDecimal annualRevenue;
        private int employeeCount;
        private EntityStatus status;
        private int totalOrders;
        private BigDecimal totalSpent;
        private Timestamp lastModified;

        // Getters and setters
        public void setId(Long id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setEmail(String email) { this.email = email; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public void setIndustryType(String industryType) { this.industryType = industryType; }
        public void setAnnualRevenue(BigDecimal annualRevenue) { this.annualRevenue = annualRevenue; }
        // Continuing CustomerProfile class...
        public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
        public void setStatus(EntityStatus status) { this.status = status; }
        public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
        public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }
        public void setLastModified(Timestamp lastModified) { this.lastModified = lastModified; }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getCompanyName() { return companyName; }
        public String getIndustryType() { return industryType; }
        public BigDecimal getAnnualRevenue() { return annualRevenue; }
        public int getEmployeeCount() { return employeeCount; }
        public EntityStatus getStatus() { return status; }
        public int getTotalOrders() { return totalOrders; }
        public BigDecimal getTotalSpent() { return totalSpent; }
        public Timestamp getLastModified() { return lastModified; }
    }

    public class ProductCatalogItem {
        private Long id;
        private String name;
        private String description;
        private String category;
        private BigDecimal price;
        private int quantityAvailable;
        private double averageRating;
        private EntityStatus status;
        private Map<String, String> attributes;
        private LocalDateTime lastUpdated;

        public ProductCatalogItem() {
            this.attributes = new HashMap<>();
        }

        public void setId(Long id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setDescription(String description) { this.description = description; }
        public void setCategory(String category) { this.category = category; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public void setQuantityAvailable(int quantityAvailable) { this.quantityAvailable = quantityAvailable; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
        public void setStatus(EntityStatus status) { this.status = status; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
        public void addAttribute(String key, String value) { this.attributes.put(key, value); }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public BigDecimal getPrice() { return price; }
        public int getQuantityAvailable() { return quantityAvailable; }
        public double getAverageRating() { return averageRating; }
        public EntityStatus getStatus() { return status; }
        public Map<String, String> getAttributes() { return attributes; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }

    public class Order {
        private Long id;
        private Long customerId;
        private LocalDateTime orderDate;
        private String status;
        private String shippingAddress;
        private String billingAddress;
        private BigDecimal totalAmount;
        private List<OrderLineItem> lineItems;
        private PaymentDetails paymentDetails;

        public Order() {
            this.lineItems = new ArrayList<>();
        }

        public void setId(Long id) { this.id = id; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
        public void setStatus(String status) { this.status = status; }
        public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
        public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public void addLineItem(OrderLineItem item) { this.lineItems.add(item); }
        public void setPaymentDetails(PaymentDetails paymentDetails) { this.paymentDetails = paymentDetails; }

        public Long getId() { return id; }
        public Long getCustomerId() { return customerId; }
        public LocalDateTime getOrderDate() { return orderDate; }
        public String getStatus() { return status; }
        public String getShippingAddress() { return shippingAddress; }
        public String getBillingAddress() { return billingAddress; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public List<OrderLineItem> getLineItems() { return lineItems; }
        public PaymentDetails getPaymentDetails() { return paymentDetails; }
    }

    public class OrderLineItem {
        private Long id;
        private Long orderId;
        private Long productId;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String productName;
        private Map<String, String> customFields;

        public OrderLineItem() {
            this.customFields = new HashMap<>();
        }

        public void setId(Long id) { this.id = id; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
        public void setProductName(String productName) { this.productName = productName; }
        public void addCustomField(String key, String value) { this.customFields.put(key, value); }

        public Long getId() { return id; }
        public Long getOrderId() { return orderId; }
        public Long getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public String getProductName() { return productName; }
        public Map<String, String> getCustomFields() { return customFields; }
    }

    public class PaymentDetails {
        private Long id;
        private Long orderId;
        private BigDecimal amount;
        private String paymentMethod;
        private String transactionId;
        private String status;
        private LocalDateTime paymentDate;
        private String paymentGateway;
        private Map<String, String> metadata;

        public PaymentDetails() {
            this.metadata = new HashMap<>();
        }

        public void setId(Long id) { this.id = id; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public void setStatus(String status) { this.status = status; }
        public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
        public void setPaymentGateway(String paymentGateway) { this.paymentGateway = paymentGateway; }
        public void addMetadata(String key, String value) { this.metadata.put(key, value); }

        public Long getId() { return id; }
        public Long getOrderId() { return orderId; }
        public BigDecimal getAmount() { return amount; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getTransactionId() { return transactionId; }
        public String getStatus() { return status; }
        public LocalDateTime getPaymentDate() { return paymentDate; }
        public String getPaymentGateway() { return paymentGateway; }
        public Map<String, String> getMetadata() { return metadata; }
    }

    public class SalesReport {
        private String groupByValue;
        private Map<String, BigDecimal> metrics;
        private LocalDateTime generatedAt;

        public SalesReport() {
            this.metrics = new HashMap<>();
            this.generatedAt = LocalDateTime.now();
        }

        public void setGroupByValue(String groupByValue) { this.groupByValue = groupByValue; }
        public void addMetric(String name, BigDecimal value) { this.metrics.put(name, value); }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        public String getGroupByValue() { return groupByValue; }
        public Map<String, BigDecimal> getMetrics() { return metrics; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public BigDecimal getMetric(String name) { return metrics.get(name); }
    }
}



