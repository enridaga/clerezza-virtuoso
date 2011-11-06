package rdf.virtuoso.storage.access;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.EntityUndeletableException;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rdf.virtuoso.storage.VirtuosoMGraph;
import virtuoso.jdbc4.VirtuosoConnection;
import virtuoso.jdbc4.VirtuosoException;
import virtuoso.jdbc4.VirtuosoResultSet;
import virtuoso.jdbc4.VirtuosoStatement;

/**
 * @scr.component
 * @scr.service 
 *              interface="org.apache.clerezza.rdf.core.access.WeightedTcProvider"
 * @scr.property name="weight" value="100"
 * @scr.property name="host" value="localhost"
 * @scr.property name="port" value="1111"
 * @scr.property name="user" value="dba"
 * @scr.property name="password" value="dba"
 * 
 * @author enridaga
 * 
 */
public class VirtuosoWeightedProvider implements WeightedTcProvider {
	/**
	 * Virtuoso JDBC Driver class
	 */
	public static final String DRIVER = "virtuoso.jdbc4.Driver";

	private static final int DEFAULT_WEIGHT = 100;

	/**
	 * MAP OF LOADED GRAPHS
	 */
	private Map<UriRef, VirtuosoMGraph> graphs = null;

	/**
	 * JDBC Connection to Virtuoso DBMS
	 */
	private VirtuosoConnection connection = null;

	/**
	 * Weight
	 */
	private int weight = DEFAULT_WEIGHT;

	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory
			.getLogger(VirtuosoWeightedProvider.class);

	/**
	 * Creates a new {@link VirtuosoWeightedProvider}.
	 * 
	 * Before the weighted provider can be used, the method
	 * <code>activate</code> has to be called.
	 */
	public VirtuosoWeightedProvider() {
		logger.info("Created VirtuosoWeightedProvider.");
	}

	/**
	 * Creates a new {@link VirtuosoWeightedProvider}
	 * 
	 * @param connection
	 */
	public VirtuosoWeightedProvider(VirtuosoConnection connection) {
		logger.info("Created VirtuosoWeightedProvider with connection parameter.");
		this.connection = connection;
	}

	/**
	 * Creates a new {@link VirtuosoWeightedProvider}
	 * 
	 * @param connection
	 * @param weight
	 */
	public VirtuosoWeightedProvider(VirtuosoConnection connection, int weight) {
		logger.info(
				"Created VirtuosoWeightedProvider with connection and weight = {}.",
				weight);
		this.weight = weight;
		this.connection = connection;
	}

	/**
	 * Activates this component.<br />
	 * 
	 * @param cCtx
	 *            Execution context of this component. A value of null is
	 *            acceptable when you set the property connection
	 * @throws ConfigurationException
	 * @throws IllegalArgumentException
	 *             No component context given and connection was not set.
	 */
	protected void activate(ComponentContext cCtx) {
		logger.info("Activating VirtuosoWeightedProvider...");
		if (cCtx == null && connection == null) {
			throw new IllegalArgumentException(
					"No component context given and connection was not set");
		} else if (cCtx != null) {
			String pid = (String) cCtx.getProperties().get(
					Constants.SERVICE_PID);
			try {
				String weightStr = (String) cCtx.getProperties().get("weight");
				try {
					this.weight = Integer.parseInt(weightStr);
				} catch (NumberFormatException nfe) {
					logger.warn(nfe.toString());
					logger.warn("Setting weight to defaults");
					this.weight = DEFAULT_WEIGHT;
				}

				/**
				 * Retrieve connection properties
				 */
				String host = (String) cCtx.getProperties().get("host");
				String port = (String) cCtx.getProperties().get("port");
				String user = (String) cCtx.getProperties().get("user");
				String pwd = (String) cCtx.getProperties().get("password");

				// Build connection string
				String connStr = new StringBuilder().append("jdbc:virtuoso://")
						.append(host).append(":").append(port).toString();
				// Init connection
				this.initConnection(connStr, user, pwd);

				logger.info("Connection initialized: {}", connStr);
				logger.info("Connection user: {}", user);
			} catch (VirtuosoException e) {
				logger.error(
						"A problem occurred while intializing connection to Virtuoso",
						e);
				logger.error("Be sure you have configured the connection parameters correctly in the OSGi/SCR configuration");
				cCtx.disableComponent(pid);
				throw new ComponentException(e.getLocalizedMessage());
			} catch (SQLException e) {
				logger.error(
						"A problem occurred while intializing connection to Virtuoso",
						e);
				logger.error("Be sure you have configured the connection parameters correctly in the OSGi/SCR configuration");
				cCtx.disableComponent(pid);
				throw new ComponentException(e.getLocalizedMessage());
			} catch (ClassNotFoundException e) {
				logger.error(
						"A problem occurred while intializing connection to Virtuoso",
						e);
				logger.error("Be sure you have configured the connection parameters correctly in the OSGi/SCR configuration");
				cCtx.disableComponent(pid);
				throw new ComponentException(e.getLocalizedMessage());
			}
		}
		logger.info("Activated VirtuosoWeightedProvider.");
	}

	/**
	 * Deactivates this component.
	 * 
	 * @param cCtx
	 *            component context provided by OSGi
	 */
	protected void deactivate(ComponentContext cCtx) {

		try {
			if (this.connection != null) {
				if (this.connection.isClosed()) {
					logger.debug("Connection is already closed");
				} else {
					logger.debug("Closing connection");
					// We close the connection
					this.connection.close();
				}
			}
		} catch (Exception re) {
			logger.warn(re.toString(), re);
			throw new RuntimeException(re);
		}
		logger.info("Shutdown complete.");
	}

	/**
	 * Initialize the JDBC connection
	 * 
	 * @param connStr
	 * @param user
	 * @param pwd
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	private void initConnection(String connStr, String user, String pwd)
			throws SQLException, ClassNotFoundException {
		if (this.connection != null) {
			this.connection.close();
		}
		/**
		 * FIXME For some reasons, it looks the DriverManager is instantiating a
		 * new virtuoso.jdbc4.Driver instance upon any activation. (Enable debug
		 * to see this in the stderr stream)
		 */
		Class.forName(VirtuosoWeightedProvider.DRIVER,true,this.getClass().getClassLoader());
		if (logger.isDebugEnabled()) {
			// FIXME! How to redirect logging to our logger???
			DriverManager.setLogWriter(new PrintWriter(System.err));

		}
		connection = (VirtuosoConnection) DriverManager.getConnection(connStr,
				user, pwd);
	}

	/**
	 * Whether the connection is active or not
	 * 
	 * @return
	 */
	public boolean isConnectionAlive() {
		if (this.connection == null) {
			logger.warn("Connection is null");
			return false;
		}
		if (this.connection.isClosed())
			return false;
		if (this.connection.isConnectionLost())
			return false;
		return true;
	}

	/**
	 * Retrieves the Graph (unmodifiable) with the given UriRef If no graph
	 * exists with such name, throws a NoSuchEntityException
	 */
	@Override
	public Graph getGraph(UriRef name) throws NoSuchEntityException {
		// If it is read-only, returns the Graph
		// If it is not read-only, returns the getGraph() version of the MGraph
		VirtuosoMGraph g = loadGraphOnce(name);
		if (g instanceof Graph) {
			return (Graph) g;
		} else {
			return g.getGraph();
		}
	}

	/**
	 * Retrieves the MGraph (modifiable) with the given UriRef. If no graph
	 * exists with such name, throws a NoSuchEntityException.
	 * 
	 * @return mgraph
	 */
	@Override
	public MGraph getMGraph(UriRef name) throws NoSuchEntityException {
		VirtuosoMGraph g = loadGraphOnce(name);
		if (g instanceof Graph) {
			// We have this graph but only in read-only mode!
			throw new NoSuchEntityException(name);
		}
		return g;
	}

	/**
	 * Load the graph once. It check whether a graph object have been alrady
	 * created for that UriRef, if yes returns it.
	 * 
	 * If not check if at least 1 triple is present in the quad for such graph
	 * identifier. If yes, creates a new graph object and loads it in the map,
	 * referring to it on next calls.
	 * 
	 * If no triples exists, the graph does not exists or it is not readable.
	 * 
	 * WARNING! THIS SHOULD BE THE ONLY METHOD TO ACCESS THE MAP graphs
	 * 
	 * @param name
	 * @return
	 */
	private VirtuosoMGraph loadGraphOnce(UriRef name) {
		logger.debug("loadGraphOnce({})", name);
		// Is it the first itme we invoke a graph here?
		if (graphs == null) {
			graphs = new HashMap<UriRef, VirtuosoMGraph>();
		}
		// Check whether the graph have been already loaded once
		if (graphs.containsKey(name)) {
			logger.debug("{} is already loaded", name);
			return graphs.get(name);
		}

		logger.debug("Attempt to load {}", name);
		// Let's create the graph object
		String SQL = "SPARQL SELECT ?G WHERE { GRAPH ?G {?A ?B ?C} . FILTER(?G = "
				+ name + ")} LIMIT 1";

		logger.debug(" SQL : {}", SQL);
		Statement st;
		try {
			st = connection.createStatement();
			st.execute(SQL);
			VirtuosoResultSet rs = (VirtuosoResultSet) st.getResultSet();
			if (rs.next() == false) {
				// The graph is empty, it is not readable or does not exists
				logger.warn("Graph does not exists: {}", name);
				throw new NoSuchEntityException(name);
			} else {
				// The graph exists and it is readable ...
				logger.debug("Graph {} is readable", name);
				// is it writable?
				logger.debug("Is {} writable?", name);
				if (canModify(name)) {
					logger.debug("Creating writable MGraph for graph {}", name);
					graphs.put(name, new VirtuosoMGraph(
							name.getUnicodeString(), connection));
				} else {
					logger.debug("Creating read-only Graph for graph {}", name);
					graphs.put(name, new VirtuosoMGraph(
							name.getUnicodeString(), connection)
							.asVirtuosoGraph());
				}
				return graphs.get(name);
			}
		} catch (VirtuosoException e) {
			logger.error("Error while executing query/connection.", e);
			throw new RuntimeException(e);
		} catch (SQLException e) {
			logger.error("Error while executing query/connection.", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Generic implementation of the get(M)Graph method. If the named graph is
	 * modifiable, behaves the same as getMGraph(UriRef name), elsewhere,
	 * behaves as getGraph(UriRef name)
	 */
	@Override
	public TripleCollection getTriples(UriRef name)
			throws NoSuchEntityException {
		return loadGraphOnce(name);
	}

	/**
	 * Returns the list of graphs in the virtuoso quad store. The returned set
	 * is unmodifiable.
	 * 
	 * @return graphs
	 */
	@Override
	public Set<UriRef> listGraphs() {
		Set<UriRef> graphs = new HashSet<UriRef>();
		// Returns the list of graphs in the virtuoso quad store
		logger.debug("listGraphs()");
		String SQL = "SPARQL SELECT DISTINCT ?G WHERE {GRAPH ?G {?S ?P ?O} }";
		try {
			VirtuosoStatement st = (VirtuosoStatement) this.connection
					.createStatement();
			VirtuosoResultSet rs = (VirtuosoResultSet) st.executeQuery(SQL);
			while (rs.next()) {
				UriRef graph = new UriRef(rs.getString(1));
				logger.debug(" Graph {}", graph);
				graphs.add(graph);
			}
		} catch (VirtuosoException e) {
			logger.error("Error while executing query/connection.", e);
			throw new RuntimeException(e);
		}
		return Collections.unmodifiableSet(graphs);
	}

	@Override
	public Set<UriRef> listMGraphs() {
		Set<UriRef> graphs = listGraphs();
		Set<UriRef> mgraphs = new HashSet<UriRef>();
		for (UriRef u : graphs) {
			if (canModify(u)) {
				mgraphs.add(u);
			}
		}
		return Collections.unmodifiableSet(mgraphs);
	}

	private long getPermissions(String graph) {
		try {
			logger.debug("getPermissions(String graph) with value {}", graph);
			ResultSet rs;
			String sql = "SELECT DB.DBA.RDF_GRAPH_USER_PERMS_GET ('" + graph
					+ "','" + connection.getMetaData().getUserName() + "') ";
			logger.debug(" SQL: {}", sql);
			Statement st = connection.createStatement();
			st.execute(sql);
			rs = st.getResultSet();
			rs.next();
			return rs.getLong(1);
		} catch (VirtuosoException e) {
			logger.error("A virtuoso SQL exception occurred.");
			throw new RuntimeException(e);
		} catch (SQLException e) {
			logger.error("An SQL exception occurred.");
			throw new RuntimeException(e);
		}
	}

	public boolean canRead(UriRef graph) {
		return (isRead(getPermissions(graph.getUnicodeString())));
	}

	public boolean canModify(UriRef graph) {
		return (isWrite(getPermissions(graph.getUnicodeString())));
	}

	private boolean testPermission(long value, int bit) {
		return BigInteger.valueOf(value).testBit(bit);
	}

	private boolean isRead(long permission) {
		return testPermission(permission, 1);
	}

	private boolean isWrite(long permission) {
		return testPermission(permission, 2);
	}

	@Override
	public Set<UriRef> listTripleCollections() {
		// I think this should behave the same as listGraphs() in our case.
		return listGraphs();
	}

	private VirtuosoMGraph createVirtuosoMGraph(UriRef name)
			throws UnsupportedOperationException, EntityAlreadyExistsException {
		// If the graph already exists, we throw an exception
		try {
			loadGraphOnce(name);
			throw new EntityAlreadyExistsException(name);
		} catch (NoSuchEntityException nsee) {
			if (canModify(name)) {
				graphs.put(name, new VirtuosoMGraph(name.getUnicodeString(),
						connection));
				return graphs.get(name);
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}

	/**
	 * Creates an initially empty MGraph. If the name already exists in the
	 * store, throws an {@see EntityAlreadyExistsException}
	 */
	@Override
	public MGraph createMGraph(UriRef name)
			throws UnsupportedOperationException, EntityAlreadyExistsException {
		return createVirtuosoMGraph(name);
	}

	/**
	 * Creates a new graph with the given triples, then returns the readable
	 * (not modifiable) version of the graph
	 * 
	 */
	@Override
	public Graph createGraph(UriRef name, TripleCollection triples)
			throws UnsupportedOperationException, EntityAlreadyExistsException {
		VirtuosoMGraph mgraph = createVirtuosoMGraph(name);
		mgraph.addAll(triples);
		return mgraph.getGraph();
	}

	/**
	 * Clears the given graph and removes it from the loaded graphs.
	 * 
	 */
	@Override
	public void deleteTripleCollection(UriRef name)
			throws UnsupportedOperationException, NoSuchEntityException,
			EntityUndeletableException {
		TripleCollection g = (VirtuosoMGraph) getTriples(name);
		if (g instanceof Graph) {
			throw new EntityUndeletableException(name);
		} else {
			((MGraph) g).clear();
			graphs.remove(name);
		}
	}

	/**
	 * Returns the names of a graph. Personally don't know why a graph should
	 * have more then 1 identifier. Anyway, this does not happen with Virtuoso
	 * 
	 * @return names
	 */
	@Override
	public Set<UriRef> getNames(Graph graph) {
		return Collections.singleton(new UriRef(((VirtuosoMGraph) graph)
				.getName()));
	}

	/**
	 * Returns the weight of this provider.
	 * 
	 */
	@Override
	public int getWeight() {
		/**
		 * The weight
		 */
		return this.weight;
	}

	/**
	 * Sets the weight
	 * 
	 * @param weight
	 */
	public void setWeight(int weight) {
		this.weight = weight;
	}
}
