package fr.ign.georeden.algorithms.graph.matching;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.naming.spi.DirStateFactory.Result;

import org.apache.http.conn.HttpHostConnectException;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.riot.RiotException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import org.apache.jena.graph.Graph;
import org.apache.jena.ontology.OntTools;
import org.apache.jena.ontology.OntTools.PredicatesFilter;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Alt;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.function.library.e;
import org.apache.jena.util.iterator.Filter;

import fr.ign.georeden.algorithms.string.StringComparisonDamLev;
import fr.ign.georeden.graph.LabeledEdge;
//import fr.ign.georeden.graph.Toponym;
import fr.ign.georeden.kb.SpatialRelationship;
import fr.ign.georeden.kb.ToponymType;
import fr.ign.georeden.utils.RDFUtil;
import fr.ign.georeden.utils.XMLUtil;

import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * The Class GraphMatching.
 */
public class GraphMatching {

	/** The logger. */
	private static Logger logger = Logger.getLogger(GraphMatching.class);

	public static final String TEI_PATH = "D:\\temp7.rdf";

	static Comparator<QuerySolutionEntry> comparatorQuerySolutionEntry = (a, b) -> {
//		return Integer.compare(a.getId(), b.getId());
		Resource r1 = null;
		if (a.getSpatialReference() != null) {
			r1 = a.getSpatialReference();
		} else {
			r1 = a.getSpatialReferenceAlt();
		}
		Resource r2 = null;
		if (b.getSpatialReference() != null) {
			r2 = b.getSpatialReference();
		} else {
			r2 = b.getSpatialReferenceAlt();
		}
		String subR1 = r1.toString().substring(r1.toString().lastIndexOf('/') + 1);
		String subR2 = r2.toString().substring(r2.toString().lastIndexOf('/') + 1);
		float fR1;
		if (subR1.indexOf('_') != -1) {
			fR1 = Float.parseFloat(subR1.substring(0, subR1.indexOf('_'))) + 0.1f;
		} else {
			fR1 = Float.parseFloat(subR1);
		}
		float fR2;
		if (subR2.indexOf('_') != -1) {
			fR2 = Float.parseFloat(subR2.substring(0, subR2.indexOf('_'))) + 0.1f;
		} else {
			fR2 = Float.parseFloat(subR2);
		}
		return Float.compare(fR1, fR2);
	};
	static Comparator<Resource> comparatorResourcePlace = (r1, r2) -> {
		String subR1 = r1.toString().substring(r1.toString().lastIndexOf('/') + 1);
		String subR2 = r2.toString().substring(r2.toString().lastIndexOf('/') + 1);
		float fR1;
		if (subR1.indexOf('_') != -1) {
			fR1 = Float.parseFloat(subR1.substring(0, subR1.indexOf('_'))) + 0.1f;
		} else {
			fR1 = Float.parseFloat(subR1);
		}
		float fR2;
		if (subR2.indexOf('_') != -1) {
			fR2 = Float.parseFloat(subR2.substring(0, subR2.indexOf('_'))) + 0.1f;
		} else {
			fR2 = Float.parseFloat(subR2);
		}
		return Float.compare(fR1, fR2);
	};
	static String ignNS = "http://example.com/namespace/";
	static String rdfsNS = "http://www.w3.org/2000/01/rdf-schema#";
	static String rdfNS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	static String propFrNS = "http://fr.dbpedia.org/property/";
	static String rlspNS = "http://data.ign.fr/def/relationsspatiales#";
	static Property rdfType = ModelFactory.createDefaultModel().createProperty(rdfNS + "type");
	static Property rdfsLabel = ModelFactory.createDefaultModel().createProperty(rdfsNS + "label");
	static Property linkSameRoute = ModelFactory.createDefaultModel().createProperty(ignNS + "linkSameRoute");
	static Property linkSameSequence = ModelFactory.createDefaultModel().createProperty(ignNS + "linkSameSequence");
	static Property linkSameBag = ModelFactory.createDefaultModel().createProperty(ignNS + "linkSameBag"); // SymmetricProperty et transitive
	static Property propNord = ModelFactory.createDefaultModel().createProperty(propFrNS + "nord");
	static Property propNordEst = ModelFactory.createDefaultModel().createProperty(propFrNS + "nordEst");
	static Property propNordOuest = ModelFactory.createDefaultModel().createProperty(propFrNS + "nordOuest");
	static Property propSud = ModelFactory.createDefaultModel().createProperty(propFrNS + "sud");
	static Property propSudEst = ModelFactory.createDefaultModel().createProperty(propFrNS + "sudEst");
	static Property propSudOuest = ModelFactory.createDefaultModel().createProperty(propFrNS + "sudOuest");
	static Property propEst = ModelFactory.createDefaultModel().createProperty(propFrNS + "est");
	static Property propOuest = ModelFactory.createDefaultModel().createProperty(propFrNS + "ouest");
	static Property rlspNorthOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "northOf");
	static Property rlspNorthEastOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "northEastOf");
	static Property rlspNorthWestOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "northWestOf");
	static Property rlspSouthOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "southOf");
	static Property rlspSouthEastOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "southEastOf");
	static Property rlspSouthWestOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "southWestOf");
	static Property rlspEastOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "eastOf");
	static Property rlspWestOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "westOf");
	/**
	 * Instantiates a new graph matching.
	 */
	private GraphMatching() {
	}

	/**
	 * Retourne un hashmap avec en clé la ressource du TEI et en valeur la liste
	 * des candidats de la KB.
	 *
	 * @return the sets the
	 */
	public static Set<Toponym> nodeSelection() {
		Integer numberOfCandidate = 10;
		float threshold = 0.6f;
		String dbPediaRdfFilePath = "D:\\dbpedia_all_with_rlsp.n3";

		logger.info("Chargement du TEI");
		Document teiSource = XMLUtil.createDocumentFromFile(TEI_PATH);
		Model teiRdf = RDFUtil.getModel(teiSource);
		logger.info("Model TEI vide : " + teiRdf.isEmpty());
		Set<Toponym> toponymsTEI = getToponymsFromTei(teiRdf);
		logger.info(toponymsTEI.size() + " toponymes dans le TEI");

		logger.info("Chargement de la KB");
		final Model kbSource = ModelFactory.createDefaultModel().read(dbPediaRdfFilePath);
		logger.info("Création du sous graphe de la KB contenant uniquement les relations spatiales");
		Model kbSubgraph = getSubGraphWithResources(kbSource);
		logger.info("Récupérations des candidats de la KB");
		final List<Candidate> candidatesFromKB = getCandidatesFromKB(kbSource);

		Set<Toponym> result = getCandidatesSelection(toponymsTEI, candidatesFromKB, numberOfCandidate, threshold);
		logger.info(result.size() + " candidats");
		
		logger.info("Préparation de la création des mini graphes pour chaques séquences");
		List<QuerySolution> querySolutions = getGraphTuples(teiRdf);
		List<QuerySolutionEntry> querySolutionEntries = getQuerySolutionEntries(querySolutions).stream().sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
		List<Resource> sequences = querySolutionEntries.stream().map(q -> q.getSequence()).distinct().collect(Collectors.toList());
		int v = 0;

		logger.info("Traitement des mini graphes des séquences");
		for (Resource sequence : sequences) {
			Model currentModel = getModelsFromSequenceV2(querySolutionEntries, sequence);
			currentModel = oneSensePerDiscourseFusion(currentModel, teiRdf);// ajouter ici la fusion des noeuds identiques (one sence per discourse)
			// Il faudrait en fait réaliser la fusion des noeuds identiques avant (au moment des transformations XSLT), amsi attention car la création
			// des mini graphes devra être adaptée (l'ordonancement des QuerySolutionEntry devra être géré d'une manière différente) 
			List<Model> alts = explodeAlts(currentModel);
//			if (alts.size() > 1) {
//				saveModelToFile("output\\georeden\\t" + v + "_original.xml", currentModel);
//				for (int j = 0; j < alts.size(); j++) {
//					saveModelToFile("output\\georeden\\t" + v + "_" + j + ".xml", alts.get(j));
//				}
//			} else 
				if (alts.size() == 1) {
				Model miniGraph = alts.get(0);
				List<IPathMatching> path = graphMatching(kbSubgraph, miniGraph, toponymsTEI, 0.4f, 0.4f, 0.2f);
				logger.info(totalCostPath(path));
			}
			v++;
		}
		
//		// fusion des données calculées (Voronoy)
//		logger.info("Chargement des RLSP calculées");
//		final Model calculsRLSPSource = ModelFactory.createDefaultModel().read("D:\\relationsSpatiales_bkp2.ttl", "TURTLE");
//		logger.info("Fusion des modèles");
//		Model newModel = ModelFactory.createDefaultModel();
//		newModel.add(kbSource);
//		newModel.add(calculsRLSPSource);
//		logger.info("Enregistrement...");
//		saveModelToFile("dbpedia_all_with_rlsp.n3", newModel, "N3");


		

		
		// il faut filter les toponyms de toponymsTEI en ne gardant que ceux qui
		// sont dans teiRDF
		// et donc il faut remplacer teiRDF par le graphe RDF d'une sequence
		
		//getRDFSequences(teiRdf);
		
		// AStarBeamSearch(sourceGraph, teiRdf, 10, numberOfCandidate,
		// toponymsTEI, candidatesFromKB);

		// logger.info("Vérification des résultats");
		// if (result.stream().anyMatch(entry ->
		// entry.getScoreCriterionToponymCandidate().isEmpty()
		// || entry.getScoreCriterionToponymCandidate().size() < 10
		// //|| entry.getName().equalsIgnoreCase("Ruffec")
		// || entry.getScoreCriterionToponymCandidate().stream().map(m ->
		// m.getValue()).max(Float::compare).get() <= 0f)) {
		// for (Iterator<Toponym> iterator = result.iterator();
		// iterator.hasNext();) {
		// Toponym key = iterator.next();
		// List<CriterionToponymCandidate> candidates =
		// key.getScoreCriterionToponymCandidate();
		// if (candidates.isEmpty() || candidates.size() < 10) {
		// logger.info("taille : " + key.getResource() + " : " +
		// candidates.size());
		// }
		//// else if (key.getName().equalsIgnoreCase("Ruffec")) {
		//// logger.info(key.getResource() + " (" + key.getName() +") : " +
		// candidates.get(0).getCandidate().getResource());
		//// }
		// else if (key.getScoreCriterionToponymCandidate().stream().map(m ->
		// m.getValue()).max(Float::compare).get() <= 0f) {
		// logger.info("scores : " + key.getResource() + " (" + key.getName()
		// +") : " + candidates.get(0).getCandidate().getResource());
		// }
		// }
		// } else {
		// logger.info("pas de topo sans candidat pour le score de label");
		// }

		return result;
	}
	
	static Model oneSensePerDiscourseFusion(Model model, Model teiRdf) {
		Model currentModel = cloneModel(model);
		Map<RDFNode, List<Statement>> typesByResources = teiRdf.listStatements(null, rdfsLabel, (RDFNode)null).toList()
			.stream().collect(Collectors.groupingBy((Statement s) -> s.getObject()));
		
		// on ne garde que les resources de la séquence actuelle
		List<Resource> resourcesFromCurrentModel = currentModel.listSubjects().toList();
		resourcesFromCurrentModel.addAll(currentModel.listObjects().toList().stream().map(n -> (Resource)n).collect(Collectors.toList()));
		resourcesFromCurrentModel = resourcesFromCurrentModel.stream().distinct().collect(Collectors.toList());
		final List<Resource> resourcesFromCurrentModel2 = new ArrayList<>(resourcesFromCurrentModel);
		for (RDFNode keyNode : typesByResources.keySet()) {
			List<Statement> oldStatements = typesByResources.get(keyNode);
			oldStatements = oldStatements.stream()
					.filter(s -> resourcesFromCurrentModel2.contains(s.getSubject())).collect(Collectors.toList());
			typesByResources.put(keyNode, oldStatements);
		}
		
		// on supprime toutes les clés avec seulement 1 élément (attention à la gestion des rdf:Alt)
		List<RDFNode> keysToRemove = typesByResources.keySet().stream().filter(k -> {
			List<Statement> statements = typesByResources.get(k);
			// prendre en compte les Alt			
			return statements.size() <= 1 || 
					(statements.size() == 2 
					&& statements.stream().anyMatch(s -> s.getSubject().toString().indexOf('_') > -1));
		}).collect(Collectors.toList());
		for (RDFNode keyToRemove : keysToRemove) {
			typesByResources.remove(keyToRemove);
		}
		if (!typesByResources.isEmpty()) {
			for (RDFNode keyNode : typesByResources.keySet()) {
				List<Resource> resources = typesByResources.get(keyNode).stream().map(m -> m.getSubject()).sorted(comparatorResourcePlace).collect(Collectors.toList());
				if (resources.stream().anyMatch(r -> r.toString().indexOf('_') > -1)) { // partie galère, car contient des Alts
					logger.info("Gérer la fusion avec les alts");
				} else {
					// firstPlace est la resource qui remplacera les autres
					Resource firstPlace = resources.get(0);
					resources.stream().filter(r -> r != firstPlace).forEach(place -> {
						List<Statement> statementsToReplace = getProperties(place, currentModel);
						List<Statement> newStatements = new ArrayList<>();
						for (Statement statement : statementsToReplace) {
							if (place == statement.getSubject()) { // on remplace le sujet
								newStatements.add(teiRdf.createStatement(firstPlace, statement.getPredicate(), statement.getObject()));
							} else {
								newStatements.add(teiRdf.createStatement((Resource)statement.getObject(), statement.getPredicate(), firstPlace));
							}
						}
						currentModel.remove(statementsToReplace);
						currentModel.add(newStatements);
					});
				}
			}
		}
		return currentModel;
	}
	static List<Statement> getProperties(Resource place, Model teiRdf) {
		List<Statement> results = teiRdf.listStatements(place, null, (RDFNode)null).toList();
		results.addAll(teiRdf.listStatements(null, null, (RDFNode)place).toList());
		return results;
	}
	/**
	 * For each rdf:Alt, return the two corresponding models
	 *
	 * @param currentModel the current model
	 * @return the list
	 */
	static List<Model> explodeAlts(Model currentModel) {
		Model currentModelClone = cloneModel(currentModel);
		List<Model> results = new ArrayList<>();
		List<Resource> alts = currentModelClone.listStatements(null, null, currentModelClone.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#Alt")).toList().stream()
			.map(s -> s.getSubject()).collect(Collectors.toList());
		results.add(currentModelClone);
		if (!alts.isEmpty()) {
			for (Resource resourceAlt : alts) {
				Alt alt = currentModelClone.getAlt(resourceAlt);
				// certaines rdf:Alt sont en fait identiques, il faut les fusionner. On vérifie si les places de cet Alt ont déjà été traités.
				// on vérifie uniquement ac la 1ère place
				Resource r_1 = (Resource)currentModelClone.getProperty(alt, currentModelClone.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#_1")).getObject();
				if (results.stream().anyMatch(m -> m.contains(r_1, linkSameBag) || m.contains(r_1, linkSameRoute) || m.contains(r_1, linkSameSequence)
						|| m.contains(null, linkSameBag, r_1) || m.contains(null, linkSameRoute, r_1) || m.contains(null, linkSameSequence, r_1))) {
					// cet rdf:Alt est un doublon
					List<Statement> statements = results.get(0).listStatements().toList().stream()
							.filter(p -> (p.getSubject().toString().equals(alt.toString()) 
								|| (p.getObject().isResource() && ((Resource)p.getObject()).toString().equals(alt.toString())))
								&& p.getPredicate().getNameSpace().equals(ignNS)).collect(Collectors.toList());
					List<Resource> places = new ArrayList<>();
					alt.iterator().toList().stream().forEach(m -> places.add((Resource)m));
					for (Resource place : places) {
						List<Statement> currentStatements = new ArrayList<>();
						for (Statement oldStatement : statements) {
							Statement newStatement;
							if (oldStatement.getSubject().getURI() == alt.getURI()) { // alt au début
								newStatement = currentModelClone.createStatement(place, oldStatement.getPredicate(), oldStatement.getObject());
							} else { // alt à la fin
								newStatement = currentModelClone.createStatement(oldStatement.getSubject(), oldStatement.getPredicate(), place);
							}
							currentStatements.add(newStatement);
						}
						for (Model m : results) {
							if (m.contains(place, linkSameBag) || m.contains(place, linkSameRoute) 
									|| m.contains(place, linkSameSequence) || m.contains(null, linkSameBag, place) 
									|| m.contains(null, linkSameRoute, place) || m.contains(null, linkSameSequence, place)) {
								for (Statement statement : currentStatements) {
									m.add(statement);
								}
							}
						}
					}
				} else { // cet rdf:Alt n'est pas un doublon
					List<Resource> places = new ArrayList<>();
					alt.iterator().toList().stream().forEach(m -> places.add((Resource)m));
					List<Statement> statements = results.get(0).listStatements().toList().stream()
							.filter(p -> (p.getSubject().toString().equals(alt.toString()) 
								|| (p.getObject().isResource() && ((Resource)p.getObject()).toString().equals(alt.toString())))
								&& p.getPredicate().getNameSpace().equals(ignNS)).collect(Collectors.toList());
					List<List<Statement>> statementLists = new ArrayList<>(); // on est censé avoir 2 list de statement (utant que de places)
					for (Resource place : places) {
						List<Statement> currentStatements = new ArrayList<>();
						for (Statement oldStatement : statements) {
							Statement newStatement;
							if (oldStatement.getSubject().getURI() == alt.getURI()) { // alt au début
								newStatement = currentModelClone.createStatement(place, oldStatement.getPredicate(), oldStatement.getObject());
							} else { // alt à la fin
								newStatement = currentModelClone.createStatement(oldStatement.getSubject(), oldStatement.getPredicate(), place);
							}
							currentStatements.add(newStatement);
						}
						statementLists.add(currentStatements);
					}
					
					List<List<Model>> resultsList = new ArrayList<>(); // autant de list de model que de places ou de list de statement
					for (int i = 0; i < places.size(); i++) {
						resultsList.add(new ArrayList<>(results));
					}
					results.clear();
					for (int i = 0; i < places.size(); i++) {
						List<Model> list = resultsList.get(i);
						List<Statement> currentStatements = statementLists.get(i);
						for (Model model : list) {
							Model newModel = cloneModel(model);
							for (Statement statement : currentStatements) {
								newModel.add(statement);
							}
							results.add(newModel);	
						}
					}
				}
				for (Model model : results) {
					deleteResource(model, alt);					
				}
			}			
		}
		return results;
	}

	public static void deleteResource(Model model, Resource resource) {
	    // remove statements where resource is subject
	    model.removeAll(resource, null, (RDFNode) null);
	    // remove statements where resource is object
	    model.removeAll(null, null, resource);
	}
	static List<QuerySolution> getGraphTuples(Model teiModel) {
		String query = 
				"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " + 
				"PREFIX iti:<http://data.ign.fr/def/itineraires#> " + 
				"SELECT ?sequence ?route ?bag ?waypoint ?spatialReference ?spatialReferenceAlt ?id WHERE { " + 
				"     ?sequence rdf:type rdf:Seq . " + 
				"     ?sequence ?pSeq ?route . " + 
				"     ?route iti:waypoints ?waypoints . " + 
				"     ?waypoints rdf:rest*/rdf:first ?bag . " + 
				"    ?bag ?pBag ?waypoint . " + 
				"    OPTIONAL { " + 
				"        ?waypoint iti:spatialReference ?spatialReference . " + 
				"        ?spatialReference <http://example.com/namespace/id> ?id . " + 
				"    }    " + 
				"    OPTIONAL { " + 
				"        ?waypoint rdf:type rdf:Alt .    " + 
				"        ?waypoint ?pWaypoint ?waypointBis .    " + 
				"        ?waypointBis iti:spatialReference ?spatialReferenceAlt .  " + 
				"        ?spatialReferenceAlt <http://example.com/namespace/id> ?id . " + 
				"    }    " + 
				"     FILTER (?pSeq != rdf:type && ?pBag != rdf:type && ?pBag != rdf:first) . " + 
				"} ORDER BY ?sequence ?route ?bag ?waypoint ?id";
		List<QuerySolution> querySolutions = new ArrayList<>();
		try {
			querySolutions.addAll(RDFUtil.getQuerySelectResults(teiModel, query));
		} catch (QueryParseException | HttpHostConnectException | RiotException | MalformedURLException
				| HttpException e) {
			logger.info(e);
		}
		return querySolutions;
	}
	static List<QuerySolutionEntry> getQuerySolutionEntries(List<QuerySolution> querySolutions) {
		List<QuerySolutionEntry> querySolutionEntries = new ArrayList<>();
		for (QuerySolution querySolution : querySolutions) {
			Resource sequence = (Resource) querySolution.get("sequence");
			Resource route = (Resource) querySolution.get("route");
			Resource bag = (Resource) querySolution.get("bag");
			Resource waypoint = (Resource) querySolution.get("waypoint");
			Resource spatialReference = (Resource) querySolution.get("spatialReference");
			Resource spatialReferenceAlt = (Resource) querySolution.get("spatialReferenceAlt");
			RDFNode nodeId = querySolution.get("id");
			Integer id = -1;
			if (nodeId.isLiteral()) {
				Literal literalId = nodeId.asLiteral();
				String stringId = literalId.getLexicalForm();
				id = Integer.parseInt(stringId);
			}
			querySolutionEntries.add(new QuerySolutionEntry(sequence, route, bag, waypoint, spatialReference, spatialReferenceAlt, id));
		}
		return querySolutionEntries;
	}
	static Model getModelsFromSequenceV2(List<QuerySolutionEntry> allQuerySolutionEntries, Resource currentSequence) {
		List<QuerySolutionEntry> querySolutionEntries = allQuerySolutionEntries.stream()
				.filter(q -> q.getSequence() == currentSequence)
				.sorted(comparatorQuerySolutionEntry)
				.collect(Collectors.toList());
		Model initialModel = ModelFactory.createDefaultModel();		
		initialModel.setNsPrefix("ign", "http://example.com/namespace/");
		initialModel = managedBags(initialModel, querySolutionEntries);
		initialModel = managedRoutes(initialModel, querySolutionEntries);
		initialModel = managedSequences(initialModel, querySolutionEntries);
		return initialModel;
	}
	static Model managedBags(Model initialModel, List<QuerySolutionEntry> querySolutionEntries) {
		for (int i = 0; i < querySolutionEntries.size() - 1; i++) {
			QuerySolutionEntry previous = null;
			if (i > 0) {
				previous = querySolutionEntries.get(i - 1);
			}
			QuerySolutionEntry current = querySolutionEntries.get(i);
			QuerySolutionEntry currentAlt = null;
			if (current.getSpatialReferenceAlt() != null) {
				i++;
				currentAlt = querySolutionEntries.get(i);
			}
			int j = i + 1;
			if(i + 1 >= querySolutionEntries.size())
				break;
			QuerySolutionEntry next = querySolutionEntries.get(j);
			QuerySolutionEntry nextAlt = null;
			if (next.getSpatialReferenceAlt() != null && j < querySolutionEntries.size() - 1) {
				j++;
				nextAlt = querySolutionEntries.get(j);
			}
			if ((previous == null || current.getBag() != previous.getBag()) && current.getBag() == next.getBag()) { 
				// current est le 1er élément d'un bag où il y a plusieurs éléments
				Resource r1;
				if (currentAlt != null) { // current est une Alt
					Alt alt = initialModel.createAlt();
					alt.add(current.getSpatialReferenceAlt());
					alt.add(currentAlt.getSpatialReferenceAlt());
					r1 = alt;
				} else { // current n'est pas une alt
					r1 = current.getSpatialReference();
				}
				while (current.getBag() == next.getBag()) {
					Resource r2;
					if (nextAlt != null) { // next est une Alt
						Alt alt = initialModel.createAlt();
						alt.add(next.getSpatialReferenceAlt());
						alt.add(nextAlt.getSpatialReferenceAlt());
						r2 = alt;
					} else { // next n'est pas une alt
						r2 = next.getSpatialReference();
					}
					initialModel.add(initialModel.createStatement(r1, linkSameBag, r2));
					if (j + 1 < querySolutionEntries.size()) {
						j++;
						next = querySolutionEntries.get(j);
						nextAlt = null;
						if (next.getSpatialReferenceAlt() != null) {
							nextAlt = querySolutionEntries.get(j + 1);
						}
					} else {
						break;
					}
				}
			}
		}
		return initialModel;
	}
	static Model managedRoutes(Model initialModel, List<QuerySolutionEntry> querySolutionEntries) {
		for (int i = 0; i < querySolutionEntries.size() - 1; i++) {
			QuerySolutionEntry current = querySolutionEntries.get(i);
			QuerySolutionEntry currentAlt = null;
			if (current.getSpatialReferenceAlt() != null) {
				i++;
				currentAlt = querySolutionEntries.get(i);
			}
			if (i + 1 >= querySolutionEntries.size())
				break;
			// on récupère les éléments du bag suivant sur la même route
			Optional<Resource> optionalBag = querySolutionEntries.subList(i + 1, querySolutionEntries.size()).stream()
				.filter(p -> p.getRoute() == current.getRoute() && p.getBag() != current.getBag())
				.map(p -> p.getBag()).distinct().findFirst();
			if (optionalBag.isPresent()) {
				List<QuerySolutionEntry> bagElements = querySolutionEntries.stream().filter(p -> p.getBag() == optionalBag.get())
						.sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
				Resource r1;
				if (currentAlt != null) { // current est une Alt
					Alt alt = initialModel.createAlt();
					alt.add(current.getSpatialReferenceAlt());
					alt.add(currentAlt.getSpatialReferenceAlt());
					r1 = alt;
				} else { // current n'est pas une alt
					r1 = current.getSpatialReference();
				}
				for (int j = 0; j < bagElements.size(); j++) {
					QuerySolutionEntry next = bagElements.get(j);
					QuerySolutionEntry nextAlt = null;
					if (next.getSpatialReferenceAlt() != null && j < bagElements.size() - 1) {
						j++;
						nextAlt = bagElements.get(j);
					}
					Resource r2;
					if (nextAlt != null) { // next est une Alt
						Alt alt = initialModel.createAlt();
						alt.add(next.getSpatialReferenceAlt());
						alt.add(nextAlt.getSpatialReferenceAlt());
						r2 = alt;
					} else { // next n'est pas une alt
						r2 = next.getSpatialReference();
					}
					initialModel.add(initialModel.createStatement(r1, linkSameRoute, r2));
				}
			}			
		}
		return initialModel;
	}

	static Model managedSequences(Model initialModel, List<QuerySolutionEntry> querySolutionEntries) {
		for (int i = 0; i < querySolutionEntries.size() - 1; i++) {
			QuerySolutionEntry current = querySolutionEntries.get(i);
			if (current.getSpatialReferenceAlt() != null) {
				i++;
			}
			if (i + 1 >= querySolutionEntries.size())
				break;
			// on vérifie que le bag est le dernier de la route
			QuerySolutionEntry next = querySolutionEntries.get(i + 1);
			if (next.getRoute() != current.getRoute()) {
				Resource lastBagOfCurrentRoute = current.getBag();
				Resource firstBagOfLastRoute = next.getBag();
				List<QuerySolutionEntry> lastBagElements = querySolutionEntries.stream().filter(p -> p.getBag() == lastBagOfCurrentRoute).sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
				List<QuerySolutionEntry> firstBagElements = querySolutionEntries.stream().filter(p -> p.getBag() == firstBagOfLastRoute).sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
				for (int j = 0; j < lastBagElements.size(); j++) {
					current = lastBagElements.get(j);
					QuerySolutionEntry currentAlt = null;
					if (current.getSpatialReferenceAlt() != null) {
						j++;
						currentAlt = lastBagElements.get(j);
					}
					Resource r1;
					if (currentAlt != null) { // current est une Alt
						Alt alt = initialModel.createAlt();
						alt.add(current.getSpatialReferenceAlt());
						alt.add(currentAlt.getSpatialReferenceAlt());
						r1 = alt;
					} else { // current n'est pas une alt
						r1 = current.getSpatialReference();
					}
					for (int k = 0; k < firstBagElements.size(); k++) {
						next = firstBagElements.get(k);
						QuerySolutionEntry nextAlt = null;
						if (next.getSpatialReferenceAlt() != null && k < firstBagElements.size() - 1) {
							k++;
							nextAlt = firstBagElements.get(k);
						}
						Resource r2;
						if (nextAlt != null) { // next est une Alt
							Alt alt = initialModel.createAlt();
							alt.add(next.getSpatialReferenceAlt());
							alt.add(nextAlt.getSpatialReferenceAlt());
							r2 = alt;
						} else { // next n'est pas une alt
							r2 = next.getSpatialReference();
						}
						initialModel.add(initialModel.createStatement(r1, linkSameSequence, r2));
					}
				}
			}	
		}
		return initialModel;
	}
	
	
	
	static Model cloneModel(Model original) {
		Model newModel = ModelFactory.createDefaultModel();
		StmtIterator iterator = original.listStatements();
		while (iterator.hasNext()) {
			Statement statement = (Statement) iterator.next();
			newModel.add(statement);
		}
		original.getNsPrefixMap().keySet().forEach(prefix -> newModel.setNsPrefix(prefix, original.getNsPrefixMap().get(prefix)));
		return newModel;
	}
	
	
	static String getType(Resource r, Model model) {
		Property type = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Statement s = r.getProperty(type);
		if (s == null)
			return null;
		RDFNode n = s.getObject();
		if (n.isLiteral()) 
			return "literal";
		Resource res = (Resource)n;
		return res.getLocalName();
	}

	static void saveModelToFile(String fileName, Model model) {
		File file = new File(fileName);
		try {
			model.write(new java.io.FileOutputStream(file));
		} catch (FileNotFoundException e) {
			logger.error(e);
		}
	}
	static void saveModelToFile(String fileName, Model model, String lang) {
		File file = new File(fileName);
		try {
			model.write(new java.io.FileOutputStream(file), lang);
		} catch (FileNotFoundException e) {
			logger.error(e);
		}
	}

	// static void fuseRDFGraphsIntoJGTGraph(Model model) {
	// SimpleDirectedGraph<Toponym, LabeledEdge<Toponym, String>> graph = new
	// SimpleDirectedGraph<>((Class<? extends LabeledEdge<Toponym, String>>)
	// LabeledEdge.class);
	//
	// }

	/**
	 * A star beam search.
	 *
	 * @param source
	 *            the source (KB subgraph with only resources linked by prop-fr
	 *            properties)
	 * @param target
	 *            the target (graph from a TEI's sequence)
	 * @param maxNumberOfNodesToProcess
	 *            the max number of nodes to process
	 * @param numberOfCandidatByToponym
	 *            the number of candidat by toponym
	 * @param toponymsTEI
	 *            the toponyms TEI
	 * @param candidates
	 *            the candidates from the KB
	 */
	static void AStarBeamSearch(Model source, Model target, int maxNumberOfNodesToProcess,
			int numberOfCandidatByToponym, Set<Toponym> toponymsTEI) {
		CriterionToponymCandidate firstC = toponymsTEI.stream().sorted((t1, t2) -> Float.compare(
				t2.getScoreCriterionToponymCandidate().stream().map(m -> m.getValue()).max(Float::compare).get(),
				t1.getScoreCriterionToponymCandidate().stream().map(m -> m.getValue()).max(Float::compare).get()))
				.limit(1).collect(Collectors.toList()).get(0).getScoreCriterionToponymCandidate().stream()
				.sorted((c1, c2) -> Float.compare(c2.getValue(), c1.getValue())).limit(1).collect(Collectors.toList())
				.get(0);
		Resource firstR = source.getResource(firstC.getCandidate().getResource());
		logger.info("CriterionToponymCandidate : " + firstC.getValue() + " / " + firstC.getCandidate().getResource());
		logger.info("CriterionToponymCandidate est dans liste des candidats : "
				+ isResourceACandidate(firstR, toponymsTEI)); // doit être faux
																// pr massif sri
																// lanka
	}

	static boolean isResourceACandidate(Resource r, Set<Toponym> toponymsTEI) {
		return toponymsTEI.stream().anyMatch(t -> t.getScoreCriterionToponymCandidate().stream()
				.anyMatch(c -> c.getCandidate().getResource().equals(r.getURI())));
	}

	static double meanNumberOfLinksByNode(Model kbSource) {
		// calcul du nombre de lien moyen par noeud (entre dbo:place)
		double result = 0;
		try {
			List<QuerySolution> sol = RDFUtil.getQuerySelectResults(kbSource,
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
							+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>"
							+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
							+ "PREFIX xml: <https://www.w3.org/XML/1998/namespace>"
							+ "PREFIX dbo: <http://dbpedia.org/ontology/>"
							+ "PREFIX ign: <http://example.com/namespace/>" + "SELECT (AVG(?count) as ?avg) WHERE {"
							+ "SELECT ?s (count(*) as ?count) WHERE {" + "    ?s ?p ?t ."
							+ "    ?t rdf:type dbo:Place ." + "  {" + "SELECT DISTINCT ?s WHERE {"
							+ "	?s rdf:type dbo:Place ." + "    }}" + "} GROUP BY ?s }");
			String avg = RDFUtil.getURIOrLexicalForm(sol.get(0), "avg");
			result = Double.parseDouble(avg);
		} catch (QueryParseException | HttpHostConnectException | RiotException | MalformedURLException
				| HttpException e) {
			logger.error(e);
		}
		return result;
	}

	/**
	 * Gets the sub graph with only the resources (no literals) linked by
	 * prop-fr.nord, prop-fr:sud, etc.
	 *
	 * @param kbSource
	 *            the kb source
	 * @return the sub graph with resources
	 */
	static Model getSubGraphWithResources(Model kbSource) {
		logger.info("Récupération du sous graphe de la base de connaissance.");
		Model model = null;
		try {
			model = RDFUtil.getQueryConstruct(kbSource, "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
					+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
					+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>" + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
					+ "PREFIX xml: <https://www.w3.org/XML/1998/namespace>"
					+ "PREFIX dbo: <http://dbpedia.org/ontology/>" + "PREFIX ign: <http://example.com/namespace/>"
					+ "CONSTRUCT {?s ?p ?o} WHERE {" + "  ?s ?p ?o."
					+ "  FILTER((?p=prop-fr:nord||?p=prop-fr:nordEst||?p=prop-fr:nordOuest||?p=prop-fr:sud"
					+ "  ||?p=prop-fr:sudEst||?p=prop-fr:sudOuest||?p=prop-fr:est||?p=prop-fr:ouest) && !isLiteral(?o))."
					+ "}", null);
			ResIterator iter = model.listSubjects();
			Set<Resource> resources = new HashSet<>();
			while (iter.hasNext()) {
				Resource r = iter.nextResource();
				resources.add(r);
			}
			NodeIterator nIter = model.listObjects();
			while (nIter.hasNext()) {
				RDFNode rdfNode = nIter.nextNode();
				if (rdfNode.isResource()) {
					Resource r = (Resource) rdfNode;
					if (!r.isAnon()) {
						resources.add(r);
					}
				}
			}
		} catch (QueryParseException | HttpHostConnectException | RiotException | MalformedURLException
				| HttpException e) {
			logger.error(e);
		}
		return model;
	}

	/**
	 * Gets the candidates selection.
	 *
	 * @param toponymsTEI
	 *            the toponyms tei
	 * @param candidatesFromKB
	 *            the candidates from kb
	 * @param threshold
	 *            the threshold
	 * @return the candidates selection
	 */
	static Set<Toponym> getCandidatesSelection(Set<Toponym> toponymsTEI, List<Candidate> candidatesFromKB,
			Integer numberOfCandidate, float threshold) {
		logger.info("Sélection des candidats (nombre de candidats : " + numberOfCandidate + ")");
		Criterion criterion = Criterion.scoreText;
		Set<Toponym> result = new HashSet<>();
		final AtomicInteger count = new AtomicInteger();
		final int total = toponymsTEI.size();
		StringComparisonDamLev sc = new StringComparisonDamLev();
		// calculs des scores pour chaque candidat de chaque toponyme
		toponymsTEI.parallelStream().filter(t -> t != null).forEach(toponym -> {
			candidatesFromKB.parallelStream().filter(c -> c != null && (toponym.getType() == ToponymType.PLACE
					|| typeContained(toponym.getType().toString(), c.getTypes()))).forEach(candidate -> {
						float score1 = sc.computeSimilarity(toponym.getName(), candidate.getName());
						float score2 = sc.computeSimilarity(toponym.getName(), candidate.getName());
						if (Math.max(score1, score2) > 0f)
							toponym.addScoreCriterionToponymCandidate(new CriterionToponymCandidate(toponym, candidate,
									Math.max(score1, score2), criterion));
					});
			if (toponym.getScoreCriterionToponymCandidate() != null
					&& !toponym.getScoreCriterionToponymCandidate().isEmpty()) {
				toponym.clearAndAddAllScoreCriterionToponymCandidate(
						toponym.getScoreCriterionToponymCandidate().stream().filter(s -> s != null)
								.sorted(Comparator.comparing(CriterionToponymCandidate::getValue).reversed())
								.filter(t -> t.getValue() >= threshold)
								.limit(Math.min(numberOfCandidate, toponym.getScoreCriterionToponymCandidate().size()))
								.collect(Collectors.toList()));
			}
			result.add(toponym);
			logger.info((count.getAndIncrement() + 1) + " / " + total);
		});
		return result;
	}

	/**
	 * Return true if the type to check is contained in the set of types.
	 *
	 * @param typeToCheck
	 *            the type to check
	 * @param types
	 *            the types
	 * @return true, if successful
	 */
	static boolean typeContained(String typeToCheck, Set<String> types) {
		String typeToponym = typeToCheck.substring(typeToCheck.lastIndexOf(':') + 1);
		for (String t : types) {
			String typeCandidate = t.substring(t.lastIndexOf('/') + 1);
			if (typeToponym.equalsIgnoreCase(typeCandidate))
				return true;
		}
		return false;
	}

	/**
	 * Gets the toponyms from tei.
	 *
	 * @return the toponyms from tei
	 */
	static Set<Toponym> getToponymsFromTei(Model teiRdf) {
		Set<Toponym> results = new HashSet<>();
		logger.info("Récupération des toponymes du TEI");
		List<QuerySolution> qSolutionsTEI = new ArrayList<>();
		try {
			qSolutionsTEI.addAll(RDFUtil.getQuerySelectResults(teiRdf,
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
							+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>"
							+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
							+ "PREFIX ign: <http://example.com/namespace/>"
							+ "PREFIX dbo: <http://dbpedia.org/ontology/>" + "" + "SELECT DISTINCT * WHERE {"
							+ "  ?s rdfs:label ?label ." + "  ?s ign:id ?id ." + "  ?s rdf:type ?type ." + "}"));
		} catch (QueryParseException | HttpHostConnectException | RiotException | MalformedURLException
				| HttpException e) {
			logger.error(e);
		}
		for (QuerySolution querySolution : qSolutionsTEI) {
			String label = RDFUtil.getURIOrLexicalForm(querySolution, "label");
			String type = RDFUtil.getURIOrLexicalForm(querySolution, "type");
			String resource = RDFUtil.getURIOrLexicalForm(querySolution, "s");
			Integer id = Integer.parseInt(RDFUtil.getURIOrLexicalForm(querySolution, "id"));
			results.add(new Toponym(resource, id, label, ToponymType.where(type)));
		}
		return results;
	}

	/**
	 * Gets the candidates from kb.
	 *
	 * @return the candidates from kb
	 */
	static List<Candidate> getCandidatesFromKB(Model kbSource) {
		List<QuerySolution> qSolutionsKB = new ArrayList<>();
		List<QuerySolution> qSolutionsTypes = new ArrayList<>();
		List<Candidate> result = new ArrayList<>();

		final Model kbSourceFinal = kbSource;

		try {
			qSolutionsKB.addAll(RDFUtil.getQuerySelectResults(kbSourceFinal,
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
							+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>"
							+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
							+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>" + "" + "SELECT DISTINCT * WHERE {"
							+ "  OPTIONAL { ?s foaf:name ?name . }" + "  OPTIONAL { ?s rdfs:label ?label . }" + "}"));
		} catch (QueryParseException | HttpHostConnectException | MalformedURLException | HttpException e) {
			logger.error(e);
		}
		logger.info("Récupérations des types des candidats de la KB");
		try {
			qSolutionsTypes.addAll(RDFUtil.getQuerySelectResults(kbSourceFinal,
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
							+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>"
							+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>" + "PREFIX dbo: <http://dbpedia.org/ontology/>"
							+ "SELECT * WHERE {" + "  ?s rdf:type ?type . "
							+ "  FILTER (?type=dbo:Mountain || ?type=dbo:Volcano || ?type=dbo:BodyOfWater || "
							+ "?type=dbo:Settlement || ?type=dbo:Territory || ?type=dbo:NaturalPlace) . " + "}"));
		} catch (QueryParseException | HttpHostConnectException | MalformedURLException | HttpException e) {
			logger.error(e);
		}
		logger.info("Traitement des types des candidats de la KB");
		List<String[]> resourceTypeMap = new ArrayList<>();
		qSolutionsTypes.stream().forEach(qs -> {
			String candidateResource = RDFUtil.getURIOrLexicalForm(qs, "s");
			String candidateType = RDFUtil.getURIOrLexicalForm(qs, "type");
			resourceTypeMap.add(new String[] { candidateResource, candidateType });
		});
		Map<String, List<String[]>> typesByResources = resourceTypeMap.stream()
				.collect(Collectors.groupingBy((String[] s) -> s[0]));
		logger.info("Traitement des candidats de la KB");
		qSolutionsKB.parallelStream().forEach(querySolution -> {
			String candidateResource = RDFUtil.getURIOrLexicalForm(querySolution, "s");
			String candidateLabel = RDFUtil.getURIOrLexicalForm(querySolution, "label");
			String candidateName = RDFUtil.getURIOrLexicalForm(querySolution, "name");
			Set<String> types = new HashSet<>();
			if (typesByResources.containsKey(candidateResource)) {
				List<String[]> list = typesByResources.get(candidateResource);
				for (String[] strings : list) {
					types.add(strings[1]);
				}
			}
			types.add("http://dbpedia.org/ontology/Place");
			result.add(new Candidate(candidateResource, candidateLabel, candidateName, types));
		});
		logger.info(result.size() + " résultats");
		return result;
	}
	/*
	 * Fonctions de coût 
	 */
	static float getDeletionCost(Resource nodeToDelete, Set<Resource> candidateResources) {
		float cost = 0f;
		if (candidateResources.contains(nodeToDelete)) {
			cost = 1f;
		}
		return cost;
	}
	static float getInsertionCost(Resource nodeToInsert) {
		return 1f;
	}
	/**
	 * Return the cost (between 0 and 1) of the substitution of the "nodeToRemove" by "nodeToInsert"
	 *
	 * @param nodeToRemove
	 * @param nodeToInsert
	 * @param labelWeight
	 * @param rlspWeight
	 * @param linkWeight
	 * @param toponymsTEI
	 * @param teiRdf
	 * @param kbWithInterestingProperties Subgraph of the kb with only "prop-fr:nord" likes nodes
	 * @return
	 */
	static float getSubstitutionCost(Resource nodeToRemove, Resource nodeToInsert, float labelWeight, float rlspWeight, float linkWeight, Set<Toponym> toponymsTEI, Model teiRdf, Model kbWithInterestingProperties) {
		// sub(n,m)=a.(1 - scoreLabel(n,m))+b.rlsp(n,m)+c.link(n,m)
		return labelWeight * (1 - scoreLabel(nodeToRemove, nodeToInsert, toponymsTEI)) + rlspWeight * scoreRlsp(nodeToRemove, nodeToInsert, teiRdf, toponymsTEI, kbWithInterestingProperties) + linkWeight * scoreLink(nodeToRemove, nodeToInsert, teiRdf, toponymsTEI, kbWithInterestingProperties);
	}
	static float scoreLabel(Resource nodeToRemove, Resource nodeToInsert, Set<Toponym> toponymsTEI) {
		List<CriterionToponymCandidate> candidates = getCandidates(nodeToInsert, toponymsTEI);
		if (!candidates.isEmpty()) {
			Optional<CriterionToponymCandidate> candidate = candidates.stream().filter(t -> t.getCandidate().getResource().equals(nodeToRemove.toString())).findFirst();
			if (candidate.isPresent()) {
				return candidate.get().getValue();
			}
		}
		return 0f;
	}
	static float scoreRlsp(Resource nodeToRemove, Resource nodeToInsert, Model teiRdf, Set<Toponym> toponymsTEI, Model kbWithInterestingProperties) {
		List<Statement> statements = getProperties(nodeToInsert, teiRdf);
		statements = keepOnlyRLSP(statements);
		if (statements.isEmpty())
			return 1f;
		float result = 0f;
		for (Statement statement : statements) {
			Resource m;
			if (statement.getSubject() == nodeToInsert) {
				m = (Resource) statement.getObject();
			} else {
				m = statement.getSubject();
			}
			Property statementProperty = statement.getPredicate();
			List<Property> properties = getCorrespondingProperties(statementProperty);
			PredicatesFilter filter = new PredicatesFilter(properties);
			Map<Resource, Integer> pathLength = new HashMap<>();
			List<CriterionToponymCandidate> candidates = getCandidates(nodeToInsert, toponymsTEI);
			for (CriterionToponymCandidate criterionToponymCandidate : candidates) {
				Resource end = kbWithInterestingProperties.getResource(criterionToponymCandidate.getCandidate().getResource());
				OntTools.Path path = OntTools.findShortestPath(kbWithInterestingProperties, nodeToRemove, end, filter);
				if (path != null) { // à revoir
					pathLength.put(end, path.size());
				}
			}
			Optional<Integer> maxPathLength = pathLength.values().stream().max(Integer::compare);
			if (maxPathLength.isPresent()) {
				result += pathLength.keySet().stream().map(key -> {
					Integer sp = pathLength.get(key);
					return (1 - scoreLabel(key, m, toponymsTEI)) * ((float)sp) / ((float)maxPathLength.get());
				}).min(Float::compareTo).get();
			}
			
		}
		
		return result / ((float)statements.size());
	}
	
	/**
	 * Gets the corresponding DBpedia properties from the TEI's one.
	 *
	 * @param teiProperty the tei property
	 * @return the corresponding properties
	 */
	static List<Property> getCorrespondingProperties(Property teiProperty) {
		List<Property> properties = new ArrayList<>(); // propriétés autorisées
		if (teiProperty == rlspNorthOf) {
			properties.add(propSud);
			properties.add(propSudEst);
			properties.add(propSudOuest);
			properties.add(propEst);
			properties.add(propOuest);
		} else if (teiProperty == rlspNorthEastOf) {
			properties.add(propSud);
			properties.add(propSudOuest);
			properties.add(propOuest);
		} else if (teiProperty == rlspNorthWestOf) {
			properties.add(propSud);
			properties.add(propSudEst);
			properties.add(propEst);
		} else if (teiProperty == rlspSouthOf) {
			properties.add(propNord);
			properties.add(propNordEst);
			properties.add(propNordOuest);
			properties.add(propEst);
			properties.add(propOuest);
		} else if (teiProperty == rlspSouthEastOf) {
			properties.add(propNord);
			properties.add(propNordOuest);
			properties.add(propOuest);
		} else if (teiProperty == rlspSouthWestOf) {
			properties.add(propNord);
			properties.add(propNordEst);
			properties.add(propEst);
		} else if (teiProperty == rlspEastOf) {
			properties.add(propNord);
			properties.add(propNordOuest);
			properties.add(propSudOuest);
			properties.add(propOuest);
			properties.add(propSud);
		}  else if (teiProperty == rlspWestOf) {
			properties.add(propNord);
			properties.add(propNordEst);
			properties.add(propSudEst);
			properties.add(propEst);
			properties.add(propSud);
		}
		return properties;
	}
	static float scoreLink(Resource nodeToRemove, Resource nodeToInsert, Model teiRdf, Set<Toponym> toponymsTEI, Model kbWithInterestingProperties) {
		float result = 0f;
		List<Statement> statements = getProperties(nodeToInsert, teiRdf);
		statements = removeRLSP(statements);
		if (statements.isEmpty())
			return result;
		
		List<Property> properties = new ArrayList<>();
		properties.add(propNord);
		properties.add(propNordEst);
		properties.add(propNordOuest);
		properties.add(propSud);
		properties.add(propSudEst);
		properties.add(propSudOuest);
		properties.add(propEst);
		properties.add(propOuest);
		PredicatesFilter filter = new PredicatesFilter(properties);
		for (Statement statement : statements) {
			Resource m;
			if (statement.getSubject() == nodeToInsert) {
				m = (Resource) statement.getObject();
			} else {
				m = statement.getSubject();
			}
			List<CriterionToponymCandidate> candidates = getCandidates(nodeToInsert, toponymsTEI);
			Map<Resource, Integer> pathLength = new HashMap<>();
			for (CriterionToponymCandidate criterionToponymCandidate : candidates) {
				Resource end = kbWithInterestingProperties.getResource(criterionToponymCandidate.getCandidate().getResource());
				OntTools.Path path = OntTools.findShortestPath(kbWithInterestingProperties, nodeToRemove, end, filter);
				pathLength.put(end, path.size());
			}
			Optional<Integer> maxPathLength = pathLength.values().stream().max(Integer::compare);
			if (maxPathLength.isPresent()) {
				result += pathLength.keySet().stream().map(key -> {
					Integer sp = pathLength.get(key);
					return (1 - scoreLabel(key, m, toponymsTEI)) * ((float)sp) / ((float)maxPathLength.get());
				}).min(Float::compareTo).get();
			}
		}
		
		return result / ((float)statements.size());
	}
	static List<CriterionToponymCandidate> getCandidates(Resource r, Set<Toponym> toponymsTEI) {
		List<CriterionToponymCandidate> results = new ArrayList<>();
		Optional<Toponym> toponym = toponymsTEI.stream().filter(t -> t.getResource().equals(r.toString())).findFirst();
		if (toponym.isPresent()) {
			return toponym.get().getScoreCriterionToponymCandidate();
			
		}
		return results;
	}
	
	/**
	 * Removes the RLSP from the statements list.
	 *
	 * @param statements the statements
	 * @return the list
	 */
	static List<Statement> removeRLSP(List<Statement>statements) {
		if (statements == null || statements.isEmpty())
			return new ArrayList<>();
		return statements.stream().filter(s -> s.getPredicate().getNameSpace().equals(rlspNS)).collect(Collectors.toList());
	}
	static List<Statement> keepOnlyRLSP(List<Statement>statements) {
		if (statements == null || statements.isEmpty())
			return new ArrayList<>();
		return statements.stream().filter(s -> !s.getPredicate().getNameSpace().equals(rlspNS)).collect(Collectors.toList());		
	}
	
	/*
	 * Algo de matching
	 * */
	static List<IPathMatching> graphMatching(Model kbSubgraph, Model miniGraph, Set<Toponym> toponymsTEI, float labelWeight, float rlspWeight, float linkWeight) {
		Resource r = miniGraph.createResource();
		// on récupère les noeuds du mini graphe
		Set<Resource> targetNodes = new HashSet<>(miniGraph.listSubjects().toList());
		targetNodes.addAll(miniGraph.listObjects().toList().stream().filter(o -> o.isResource()).map(m -> (Resource)m).collect(Collectors.toList()));
		// On commence par ne garder que les toponyms présents dans ce mini graphe
		Set<Toponym> toponymsSeq = new HashSet<>(toponymsTEI.stream().filter(p -> targetNodes.stream().anyMatch(res -> p.getResource().equals(res.toString()))).collect(Collectors.toList()));
		
		// on récupère les noeuds de dbpedia à traiter
		Set<Resource> sourceNodes = new HashSet<>(
				toponymsSeq.stream()
				.map(m -> m.getScoreCriterionToponymCandidate().stream()
						.map(l -> kbSubgraph.createResource(l.getCandidate().getResource())).collect(Collectors.toList()))
			        .flatMap(l -> l.stream()).distinct()
			        .collect(Collectors.toList()));
		List<List<IPathMatching>> open = new ArrayList<>();
		Resource firstSourceNode = kbSubgraph.createResource(toponymsSeq.stream().sorted((a, b) -> Integer.compare(a.getScoreCriterionToponymCandidate().size(), b.getScoreCriterionToponymCandidate().size())).findFirst().get().getScoreCriterionToponymCandidate().get(0).getCandidate().getResource());// null; // Définir comment on choisit ce noeud
		for (Resource targetNode : targetNodes) {
			float cost = getSubstitutionCost(firstSourceNode, targetNode, labelWeight, rlspWeight, linkWeight, toponymsSeq, miniGraph, kbSubgraph);
			List<IPathMatching> path = new ArrayList<>();
			path.add(new Substitution(firstSourceNode, targetNode, cost));
			open.add(path);
		}
		List<IPathMatching> pathDeletion = new ArrayList<>();
		pathDeletion.add(new Deletion(firstSourceNode, 1));
		open.add(pathDeletion);
		List<IPathMatching> pMin = null;
		while (true) {
			pMin = open.stream().min((a,b) -> Float.compare(totalCostPath(a) + 
					heuristicCostPath(a,  kbSubgraph, miniGraph, toponymsSeq, labelWeight, rlspWeight, linkWeight, getSourceUnusedResources(a, sourceNodes), getTargetUnusedResources(a, targetNodes)), 
					totalCostPath(b) + 
					heuristicCostPath(b,  kbSubgraph, miniGraph, toponymsSeq, labelWeight, rlspWeight, linkWeight, getSourceUnusedResources(b, sourceNodes), getTargetUnusedResources(b, targetNodes)))).get(); // définir pmin g + h
			open.remove(pMin);
			if (isCompletePath(pMin, sourceNodes, targetNodes)) {
				break;
			} else {
				//Set<Resource> usedResourcesFromTarget = getTargetUsedResources(pMin); // resources du graphe cible utilisées dans ce chemin
				Set<Resource> unusedResourcesFromTarget = getTargetUnusedResources(pMin, targetNodes);
				if (pMin.size() < sourceNodes.size()) {
					final List<IPathMatching> pMinFinal = new ArrayList<>(pMin);
					Resource currentSourceNode = kbSubgraph.createResource(toponymsSeq.stream()
							.filter(p -> getTargetUnusedResources(pMinFinal, targetNodes).stream().anyMatch(t -> t.toString().equals(p)))
							.sorted((a, b) -> Integer.compare(a.getScoreCriterionToponymCandidate().size(), b.getScoreCriterionToponymCandidate().size())).findFirst().get().getScoreCriterionToponymCandidate().get(0).getCandidate().getResource());//null; // Définir comment on choisit ce noeud
					for (Resource resource : unusedResourcesFromTarget) {
						List<IPathMatching> newPath = new ArrayList<>(pMin);
						float cost = getSubstitutionCost(currentSourceNode, resource, labelWeight, rlspWeight, linkWeight, toponymsSeq, miniGraph, kbSubgraph);
						newPath.add(new Substitution(currentSourceNode, resource, cost));
						open.add(newPath);
					}
					List<IPathMatching> newPathDeletion = new ArrayList<>(pMin);
					newPathDeletion.add(new Deletion(currentSourceNode, 1));
					open.add(newPathDeletion);
				} else {
					for (Resource resource : unusedResourcesFromTarget) {
						List<IPathMatching> newPath = new ArrayList<>(pMin);
						newPath.add(new Insertion(resource, getInsertionCost(resource)));
						open.add(newPath);
					}
				}
			}
		}
		
		return pMin;
	}
	
	/**
	 * Gets the used resources of the target graph in the current path.
	 *
	 * @param path the path
	 * @return the target resources used
	 */
	static Set<Resource> getTargetUsedResources(List<IPathMatching> path) {
		Set<Resource> usedResources = new HashSet<>();
		for (IPathMatching pathElement : path) {
			if (pathElement.getClass() == Substitution.class) {
				Substitution sub = (Substitution) pathElement;
				usedResources.add(sub.getInsertedNode());
			} else if (pathElement.getClass() == Insertion.class) {
				Insertion sub = (Insertion) pathElement;
				usedResources.add(sub.getInsertedNode());
			}
		}
		return usedResources;
	}
	static Set<Resource> getTargetUnusedResources(List<IPathMatching> path, Set<Resource> targetResources) {
		Set<Resource> unusedResources = new HashSet<>(targetResources);
		unusedResources.removeAll(getTargetUsedResources(path));
		return unusedResources;
	}
	
	/**
	 * Gets the used resources of the source graph in the current path.
	 *
	 * @param path the path
	 * @return the source used resources
	 */
	static Set<Resource> getSourceUsedResources(List<IPathMatching> path) {
		Set<Resource> usedResources = new HashSet<>();
		for (IPathMatching pathElement : path) {
			if (pathElement.getClass() == Substitution.class) {
				Substitution sub = (Substitution) pathElement;
				usedResources.add(sub.getDeletedNode());
			} else if (pathElement.getClass() == Deletion.class) {
				Deletion sub = (Deletion) pathElement;
				usedResources.add(sub.getDeletedNode());
			}
		}
		return usedResources;
	}

	static Set<Resource> getSourceUnusedResources(List<IPathMatching> path, Set<Resource> sourceResources) {
		Set<Resource> unusedResources = new HashSet<>(sourceResources);
		unusedResources.removeAll(getTargetUsedResources(path));
		return unusedResources;
	}
	static boolean isCompletePath(List<IPathMatching> path, Set<Resource> sourceNodes, Set<Resource> targetNodes) {
		Set<Resource> usedNodesFromTarget = getTargetUsedResources(path);
		Set<Resource> usedNodesFromSource = getSourceUsedResources(path);
		return sourceNodes.stream().allMatch(sn -> usedNodesFromSource.contains(sn)) && targetNodes.stream().allMatch(tn -> usedNodesFromTarget.contains(tn));
	}
	static interface IPathMatching {
		float getCost();
	}
	static class Insertion implements IPathMatching {
		private Resource insertedNode;
		private float cost;
		public Insertion(Resource insertedNode, float cost) {
			this.insertedNode = insertedNode;
			this.cost = cost;
		}
		public Resource getInsertedNode() {
			return this.insertedNode;
		}
		@Override
		public float getCost() {
			return this.cost;
		}
	}
	static class Deletion implements IPathMatching {
		private Resource deletedNode;
		private float cost;
		public Deletion(Resource deletedNode, float cost) {
			this.deletedNode = deletedNode;
			this.cost = cost;
		}
		public Resource getDeletedNode() {
			return this.deletedNode;
		}
		@Override
		public float getCost() {
			return this.cost;
		}
	}
	static class Substitution implements IPathMatching {
		private Resource deletedNode;
		private Resource insertedNode;
		private float cost;
		public Substitution(Resource deletedNode, Resource insertedNode, float cost) {
			this.deletedNode = deletedNode;
			this.cost = cost;
		}
		public Resource getDeletedNode() {
			return this.deletedNode;
		}
		public Resource getInsertedNode() {
			return this.insertedNode;
		}
		@Override
		public float getCost() {
			return this.cost;
		}
	}
	
	/**
	 * Total cost path. (g)
	 *
	 * @param path the path
	 * @return the float
	 */
	static float totalCostPath(List<IPathMatching> path) {
		float result = 0f;
		if (path == null)
			return result;
		for (IPathMatching iPathMatching : path) {
			result += iPathMatching.getCost();
		}
		return result;
	}
	/**
	 * Heuristic cost of the path. (h)
	 * 
	 * @param path
	 * @return
	 */
	static float heuristicCostPath(List<IPathMatching> path,  Model kbSubgraph, Model miniGraph, Set<Toponym> toponymsTEI, float labelWeight, float rlspWeight, float linkWeight, Set<Resource> unusedSourceNodes, Set<Resource> unusedTargetNodes) {
		float result = 0f;
		/*
		 * h(o)=
		 * somme des min{n1, n2} substitutions les moins chères + 
		 * max{0, n1 − n2} suppression + 
		 * max{0, n2 − n1} insertions (si l’on pose n1=nombre de noeuds non traités du graphe source et n2=nombre de noeuds non traités du graphe cible).
		 */
		Integer n1 = unusedSourceNodes.size();
		Integer n2 = unusedTargetNodes.size();
		List<Float> substitutionCosts = new ArrayList<>();
		for (Resource unusedSourceNode : unusedSourceNodes) {
			for (Resource unusedTargetNode : unusedTargetNodes) {
				substitutionCosts.add(
						getSubstitutionCost(unusedSourceNode, unusedTargetNode, labelWeight, rlspWeight, linkWeight, toponymsTEI, miniGraph, kbSubgraph));
			}
		}
		result += substitutionCosts.stream().sorted((a, b) -> Float.compare(b, a)).limit(Integer.min(n1, n2)).mapToDouble(i -> i).sum();
		result += (float)Integer.max(0, n1 - 2) + (float)Integer.max(0, n2 - n1);
		return result;
	}
	static class CustomPropertyComparator implements Comparator<Property>{

	    @Override
	    public int compare(Property p1, Property p2) {
	    	String str1 = p1.getLocalName();
	    	String str2 = p2.getLocalName();
	       // extract numeric portion out of the string and convert them to int
	       // and compare them, roughly something like this

	       int num1 = Integer.parseInt(str1.substring(1));
	       int num2 = Integer.parseInt(str2.substring(1));

	       return num1 - num2;

	    }
	}
	static class CustomResourceComparator implements Comparator<Resource>{

	    @Override
	    public int compare(Resource p1, Resource p2) {
	    	String str1 = p1.toString();
	    	String str2 = p2.toString();

	       return str1.compareTo(str2);

	    }
	}
	
	static class QuerySolutionEntry {
		private Resource sequence;
		private Resource route;
		private Resource bag;
		private Resource waypoint;
		private Resource spatialReference;
		private Resource spatialReferenceAlt;
		private Integer id;
		public QuerySolutionEntry(Resource sequence, Resource route, Resource bag, Resource waypoint, Resource spatialReference, Resource spatialReferenceAlt, Integer id) {
			this.sequence = sequence;
			this.route = route;
			this.bag = bag;
			this.waypoint = waypoint;
			this.spatialReference = spatialReference;
			this.spatialReferenceAlt = spatialReferenceAlt;
			this.id = id;
		}
		
		public Resource getSequence() {
			return this.sequence;
		}
		public Resource getRoute() {
			return this.route;
		}
		public Resource getBag() {
			return this.bag;
		}
		public Resource getWaypoint() {
			return this.waypoint;
		}
		public Resource getSpatialReference() {
			return this.spatialReference;
		}
		public Resource getSpatialReferenceAlt() {
			return this.spatialReferenceAlt;
		}
		public Integer getId() {
			return this.id;
		}
	}
}
