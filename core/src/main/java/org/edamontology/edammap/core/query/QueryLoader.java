/*
 * Copyright © 2016, 2017, 2018, 2020 Erik Jaaniso
 *
 * This file is part of EDAMmap.
 *
 * EDAMmap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EDAMmap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EDAMmap.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.edamontology.edammap.core.query;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.edamontology.pubfetcher.core.common.IllegalRequestException;
import org.edamontology.pubfetcher.core.common.PubFetcher;
import org.edamontology.pubfetcher.core.db.DatabaseEntryType;
import org.edamontology.pubfetcher.core.db.publication.PublicationIds;

import org.edamontology.edammap.core.edam.Branch;
import org.edamontology.edammap.core.edam.Concept;
import org.edamontology.edammap.core.edam.EdamUri;
import org.edamontology.edammap.core.input.Csv;
import org.edamontology.edammap.core.input.InputType;
import org.edamontology.edammap.core.input.Json;
import org.edamontology.edammap.core.input.ServerInput;
import org.edamontology.edammap.core.input.Xml;
import org.edamontology.edammap.core.input.csv.Bioconductor;
import org.edamontology.edammap.core.input.csv.Generic;
import org.edamontology.edammap.core.input.csv.Msutils;
import org.edamontology.edammap.core.input.csv.SEQwiki;
import org.edamontology.edammap.core.input.json.DocumentationType;
import org.edamontology.edammap.core.input.json.DownloadType;
import org.edamontology.edammap.core.input.json.Edam;
import org.edamontology.edammap.core.input.json.Function;
import org.edamontology.edammap.core.input.json.InputOutput;
import org.edamontology.edammap.core.input.json.LinkType;
import org.edamontology.edammap.core.input.json.Publication;
import org.edamontology.edammap.core.input.json.PublicationType;
import org.edamontology.edammap.core.input.json.Tool;
import org.edamontology.edammap.core.input.xml.Biotools14;

public class QueryLoader {

	private static final Logger logger = LogManager.getLogger();

	private static final String SEQWIKI = "http://seqanswers.com/wiki/";
	private static final String BIOC_VIEWS = "https://bioconductor.org/packages/release/BiocViews.html#___";
	public static final String BIOTOOLS = "https://bio.tools/";
	private static final String SERVER = QueryType.server.name();

	private static final Pattern INTERNAL_SEPARATOR_BAR = Pattern.compile("\\|");
	private static final Pattern INTERNAL_SEPARATOR_COMMA = Pattern.compile(",");
	private static final Pattern INTERNAL_SEPARATOR_NEWLINE = Pattern.compile("\n");
	private static final Pattern PUBLICATION_ID_SEPARATOR = Pattern.compile("\t");
	private static final Pattern BIOTOOLS_LINKS_EXCLUDE = Pattern.compile("(?i)^https?://(www\\.)?(bioconductor\\.org|git\\.bioconductor\\.org/+packages/+.*|cbs\\.dtu\\.dk/+services|expasy\\.org|ms-utils\\.org|emboss\\.open-bio\\.org/+html/+adm/+ch01s01\\.html|rostlab\\.org/+owiki/+index\\.php/+Packages|bioconductor/+packages/+release/+bioc/+src/+contrib/+[^/]+\\.tar\\.gz)/*$");

	private static Stream<String> split(String toSplit) {
		if (toSplit == null) return null;
		if (toSplit.trim().isEmpty()) return Stream.empty();
		return INTERNAL_SEPARATOR_BAR.splitAsStream(toSplit);
	}
	private static Stream<String> split(String toSplit, Pattern pattern) {
		if (toSplit == null) return null;
		if (toSplit.trim().isEmpty()) return Stream.empty();
		return pattern.splitAsStream(toSplit);
	}

	private static List<Keyword> keywords(Stream<String> keywords, String type, String url, int max) {
		if (keywords == null) return null;
		List<Keyword> keywordsList = keywords
			.filter(Objects::nonNull)
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.map(s -> new Keyword(type, s, url != null ? url + s.replaceAll(" ", "_") : null))
			.collect(Collectors.toList());
		if (max > 0 && keywordsList.size() > max) {
			throw new IllegalRequestException("Number of keywords (" + keywordsList.size() + ") is greater than maximum allowed (" + max + ")");
		}
		return keywordsList;
	}
	private static List<Keyword> keywordsCamelCase(Stream<String> keywords, String type, String url) {
		if (keywords == null) return null;
		return keywords
			.filter(Objects::nonNull)
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.map(s -> s.replaceAll("(?<=[^A-Z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|(?<=[A-Za-z])(?=[^A-Za-z])", " "))
			.map(s -> new Keyword(type, s, url != null ? url + s.replaceAll(" ", "") : null))
			.collect(Collectors.toList());
	}

	private static Link link(String link, String type, boolean throwException) {
		String url = PubFetcher.getUrl(link, throwException);
		if (url != null) {
			return new Link(url, type);
		} else {
			return null;
		}
	}
	private static List<Link> links(Stream<String> links, String type, boolean throwException, int max) {
		if (links == null) return null;
		List<Link> linksList = links
			.map(s -> link(s, type, throwException))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		if (max > 0 && linksList.size() > max) {
			throw new IllegalRequestException("Number of links (" + linksList.size() + ") is greater than maximum allowed (" + max + ")");
		}
		return linksList;
	}

	private static Collection<Link> addLink(String link, String type, boolean throwException, Collection<Link> collection) {
		Link newLink = link(link, type, throwException);
		if (newLink != null) {
			collection.add(newLink);
		}
		return collection;
	}

	private static List<Link> linksJson(Stream<? extends org.edamontology.edammap.core.input.json.Link<?>> links, List<?> types, boolean throwException) {
		return links
			.filter(l -> !Collections.disjoint(l.getType(), types))
			.filter(l -> !BIOTOOLS_LINKS_EXCLUDE.matcher(l.getUrl().trim()).matches())
			.map(l -> link(l.getUrl(), l.toStringType(), throwException))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}
	private static List<Link> downloadLinksJson(Stream<? extends org.edamontology.edammap.core.input.json.LinkDownload> links, List<DownloadType> types, boolean throwException) {
		return links
			.filter(l -> types.contains(l.getType()))
			.filter(l -> !BIOTOOLS_LINKS_EXCLUDE.matcher(l.getUrl().trim()).matches())
			.map(l -> link(l.getUrl(), l.getType().toString(), throwException))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private static PublicationIdsQuery publicationId(String publicationId, String url, String type, boolean throwException) {
		PublicationIds publicationIds = PubFetcher.getPublicationIds(publicationId, url, throwException);
		if (publicationIds != null) {
			return new PublicationIdsQuery(publicationIds, type);
		} else {
			return null;
		}
	}
	private static List<PublicationIdsQuery> publicationIds(Stream<String> publicationIds, String url, String type, boolean throwException, int max) {
		if (publicationIds == null) return null;
		List<PublicationIdsQuery> publicationIdsList = publicationIds
			.map(s -> publicationId(s, url, type, throwException))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		if (max > 0 && publicationIdsList.size() > max) {
			throw new IllegalRequestException("Number of publication IDs (" + publicationIdsList.size() + ") is greater than maximum allowed (" + max + ")");
		}
		return publicationIdsList;
	}

	private static Collection<PublicationIdsQuery> addPublicationId(String publicationId, String url, String type, boolean throwException, Collection<PublicationIdsQuery> collection) {
		PublicationIdsQuery newPublicationId = publicationId(publicationId, url, type, throwException);
		if (newPublicationId != null) {
			collection.add(newPublicationId);
		}
		return collection;
	}

	private static PublicationIdsQuery publicationId(String pmid, String pmcid, String doi, String url, String type, boolean throwException, boolean logEmpty) {
		PublicationIds publicationIds = PubFetcher.getPublicationIds(pmid, pmcid, doi, url, url, url, throwException, logEmpty);
		if (publicationIds != null) {
			return new PublicationIdsQuery(publicationIds, type);
		} else {
			return null;
		}
	}
	private static List<PublicationIdsQuery> publicationIds(List<List<String>> publicationIds, String url, String type, boolean throwException, boolean logEmpty, int max) {
		if (publicationIds == null) return null;
		List<PublicationIdsQuery> publicationIdsList = new ArrayList<>();
		for (List<String> pubIds : publicationIds) {
			PublicationIdsQuery publicationIdsQuery;
			if (pubIds.size() == 1) {
				publicationIdsQuery = publicationId(pubIds.get(0), url, type, throwException);
			} else if (pubIds.size() == 3) {
				publicationIdsQuery = publicationId(pubIds.get(0), pubIds.get(1), pubIds.get(2), url, type, throwException, logEmpty);
			} else {
				throw new IllegalRequestException("Publication ID has illegal number of parts (" + pubIds.size() + ")" + (pubIds.size() > 0 ? ", first part is " + pubIds.get(0) : ""));
			}
			if (publicationIdsQuery != null) {
				publicationIdsList.add(publicationIdsQuery);
			}
		}
		if (max > 0 && publicationIdsList.size() > max) {
			throw new IllegalRequestException("Number of publication IDs (" + publicationIdsList.size() + ") is greater than maximum allowed (" + max + ")");
		}
		return publicationIdsList;
	}

	private static Collection<PublicationIdsQuery> addPublicationId(String pmid, String pmcid, String doi, String url, String type, boolean throwException, boolean logEmpty, Collection<PublicationIdsQuery> collection) {
		PublicationIdsQuery newPublicationId = publicationId(pmid, pmcid, doi, url, type, throwException, logEmpty);
		if (newPublicationId != null) {
			collection.add(newPublicationId);
		}
		return collection;
	}

	private static boolean checkEdamUri(EdamUri edamUri, Map<EdamUri, Concept> concepts) {
		if (concepts != null && concepts.get(edamUri) == null) {
			throw new IllegalRequestException("Non-existent EDAM URI: " + edamUri);
		}
		return true;
	}

	private static Set<EdamUri> edamUris(Stream<String> annotations, Map<EdamUri, Concept> concepts) {
		if (annotations == null) return null;
		return annotations
			.filter(Objects::nonNull)
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.map(s -> (s.contains("/") ? s : EdamUri.DEFAULT_PREFIX + "/" + s))
			.map(s -> new EdamUri(s.toLowerCase(Locale.ROOT), EdamUri.DEFAULT_PREFIX))
			.filter(e -> checkEdamUri(e, concepts))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static EdamUri edamUriJson(Edam edam, Map<EdamUri, Concept> concepts) {
		if (edam.getUri() != null && !edam.getUri().trim().isEmpty()) {
			EdamUri edamUri = new EdamUri(edam.getUri().trim().toLowerCase(Locale.ROOT), EdamUri.DEFAULT_PREFIX);
			if (!checkEdamUri(edamUri, concepts)) return null;
			return edamUri;
		}
		if (edam.getTerm() != null && !edam.getTerm().trim().isEmpty()) {
			String term = edam.getTerm().trim();
			if (concepts != null) {
				for (Entry<EdamUri, Concept> e : concepts.entrySet()) {
					if (e.getValue().getLabel().equals(term)) return e.getKey();
				}
				for (Entry<EdamUri, Concept> e : concepts.entrySet()) {
					if (e.getValue().getExactSynonyms().contains(term)) return e.getKey();
				}
				for (Entry<EdamUri, Concept> e : concepts.entrySet()) {
					if (e.getValue().getNarrowSynonyms().contains(term)) return e.getKey();
					if (e.getValue().getBroadSynonyms().contains(term)) return e.getKey();
				}
			}
			logger.warn("Can't find EDAM URI for term {} in JSON", term);
			return null;
		} else {
			logger.warn("An EDAM object is empty in JSON");
			return null;
		}
	}
	private static Set<EdamUri> edamUrisJson(Stream<Edam> annotations, Map<EdamUri, Concept> concepts) {
		return annotations
			.map(e -> edamUriJson(e, concepts))
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static Query getGeneric(Generic generic, Map<EdamUri, Concept> concepts, String filename) {
		return new Query(
			generic.getId() != null ? generic.getId().trim() : null,
			generic.getName().trim(),
			keywords(split(generic.getKeywords()), "Keywords", null, 0),
			generic.getDescription() != null ? generic.getDescription().trim() : null,
			links(split(generic.getWebpageUrls()), null, false, 0),
			links(split(generic.getDocUrls()), null, false, 0),
			publicationIds(split(generic.getPublicationIds()), filename, null, false, 0),
			edamUris(split(generic.getAnnotations()), concepts));
	}

	private static EdamUri getSEQwikiAnnotation(Keyword keyword, Map<EdamUri, Concept> concepts) {
		if (concepts == null) return null;
		for (Map.Entry<EdamUri, Concept> concept : concepts.entrySet()) {
			if (((keyword.getType().equals("Domain") && concept.getKey().getBranch() == Branch.topic)
				|| (keyword.getType().equals("Method") && concept.getKey().getBranch() == Branch.operation))
				&& keyword.getValue().equalsIgnoreCase(concept.getValue().getLabel())) {
				return concept.getKey();
			}
		}
		return null;
	}
	private static Query getSEQwiki(SEQwiki SEQwiki, Map<EdamUri, Concept> concepts, String filename) {
		List<Keyword> keywords = new ArrayList<>();
		List<Keyword> domainKeywords = keywords(split(SEQwiki.getDomains(), INTERNAL_SEPARATOR_COMMA), "Domain", SEQWIKI, 0);
		if (domainKeywords != null) {
			keywords.addAll(domainKeywords);
		}
		List<Keyword> methodKeywords = keywords(split(SEQwiki.getMethods(), INTERNAL_SEPARATOR_COMMA), "Method", SEQWIKI, 0);
		if (methodKeywords != null) {
			keywords.addAll(methodKeywords);
		}

		List<Link> webpageUrls = new ArrayList<>();
		addLink(SEQWIKI + SEQwiki.getName().trim().replace(" ", "_"), null, false, webpageUrls);
		List<Link> webpagesLinks = links(split(SEQwiki.getWebpages(), INTERNAL_SEPARATOR_COMMA), null, false, 0);
		if (webpagesLinks != null) {
			webpageUrls.addAll(webpagesLinks);
		}

		Set<EdamUri> annotations = new LinkedHashSet<>();
		for (Keyword keyword : keywords) {
			EdamUri annotation = getSEQwikiAnnotation(keyword, concepts);
			if (annotation != null) annotations.add(annotation);
		}

		return new Query(
			null,
			SEQwiki.getName().trim(),
			keywords,
			SEQwiki.getSummary().trim(),
			webpageUrls,
			links(split(SEQwiki.getDocs(), INTERNAL_SEPARATOR_COMMA), null, false, 0),
			publicationIds(split(SEQwiki.getPublications(), INTERNAL_SEPARATOR_COMMA), filename, null, false, 0),
			annotations);
	}

	private static Query getMsutils(Msutils msutils, Map<EdamUri, Concept> concepts, String filename) {
		List<Link> webpageUrls = new ArrayList<>();
		addLink(msutils.getWeblink(), "Weblink", false, webpageUrls);
		addLink(msutils.getLink(), "Link", false, webpageUrls);

		Set<EdamUri> annotations = new LinkedHashSet<>();
		Set<EdamUri> topicUris = edamUris(split(msutils.getTopic()), concepts);
		if (topicUris != null) {
			annotations.addAll(topicUris);
		}
		Set<EdamUri> operationUris = edamUris(split(msutils.getOperation()), concepts);
		if (operationUris != null) {
			annotations.addAll(operationUris);
		}
		Set<EdamUri> formatInUris = edamUris(split(msutils.getFormat_in()), concepts);
		if (formatInUris != null) {
			annotations.addAll(formatInUris);
		}
		Set<EdamUri> formatOutUris = edamUris(split(msutils.getFormat_out()), concepts);
		if (formatOutUris != null) {
			annotations.addAll(formatOutUris);
		}

		return new Query(
			null,
			msutils.getName().trim(),
			null,
			msutils.getDescription().trim(),
			webpageUrls,
			null,
			publicationIds(split(msutils.getPaper()), filename, null, false, 0),
			annotations);
	}

	private static Query getBioconductor(Bioconductor bioconductor, Map<EdamUri, Concept> concepts) {
		List<Link> webpageUrls = null;
		if (bioconductor.getReposFullUrl() != null && !bioconductor.getReposFullUrl().trim().isEmpty()) {
			webpageUrls = new ArrayList<>();
			addLink(bioconductor.getReposFullUrl(), null, false, webpageUrls);
		}

		List<Link> docUrls = null;
		if (bioconductor.getReposFullUrl() != null && !bioconductor.getReposFullUrl().trim().isEmpty()) {
			docUrls = new ArrayList<>();
			addLink(bioconductor.getReposFullUrl().replaceFirst("/bioc/html/(.+)\\.html", "/bioc/manuals/$1/man/$1.pdf"), null, false, webpageUrls);
			addLink(bioconductor.getReposFullUrl().replaceFirst("/bioc/html/(.+)\\.html", "/bioc/vignettes/$1/inst/doc/$1.pdf"), null, false, webpageUrls);
			addLink(bioconductor.getReposFullUrl().replaceFirst("/bioc/html/(.+)\\.html", "/bioc/vignettes/$1/inst/doc/$1.html"), null, false, webpageUrls);
			addLink(bioconductor.getReposFullUrl().replaceFirst("/bioc/html/(.+)\\.html", "/bioc/vignettes/$1/inst/doc/$1-vignette.pdf"), null, false, webpageUrls);
		}

		Set<EdamUri> annotations = new LinkedHashSet<>();
		Set<EdamUri> topicUris = edamUris(split(bioconductor.getTopic_URI()), concepts);
		if (topicUris != null) {
			annotations.addAll(topicUris);
		}
		Set<EdamUri> operationUris = edamUris(split(bioconductor.getOperation_URI()), concepts);
		if (operationUris != null) {
			annotations.addAll(operationUris);
		}

		return new Query(
			null,
			bioconductor.getName().trim() + " : " + bioconductor.getTitle().trim().replaceAll("\n", " "),
			keywordsCamelCase(split(bioconductor.getBiocViews()), "biocViews", BIOC_VIEWS),
			bioconductor.getDescription().trim(),
			webpageUrls,
			docUrls,
			null,
			annotations);
	}

	private static Query getBiotools14(Biotools14 biotools, Map<EdamUri, Concept> concepts, String filename) {
		List<Link> webpageUrls = new ArrayList<>();
		addLink(biotools.getHomepage(), "Homepage", false, webpageUrls);
		List<Link> mirrorLinks = links(biotools.getMirrors().stream(), "Mirror", false, 0);
		if (mirrorLinks != null) {
			webpageUrls.addAll(mirrorLinks);
		}

		List<Link> docUrls = new ArrayList<>();
		addLink(biotools.getDocsHome(), "Home", false, docUrls);
		addLink(biotools.getDocsGithub(), "Github", false, docUrls);

		List<PublicationIdsQuery> publicationIds = new ArrayList<>();
		addPublicationId(biotools.getPublicationsPrimaryID(), filename, PublicationType.PRIMARY.toString(), false, publicationIds);
		List<PublicationIdsQuery> otherPublicationIds = publicationIds(biotools.getPublicationsOtherIDs().stream(), filename, PublicationType.OTHER.toString(), false, 0);
		if (otherPublicationIds != null) {
			publicationIds.addAll(publicationIds(biotools.getPublicationsOtherIDs().stream(), filename, PublicationType.OTHER.toString(), false, 0));
		}

		Set<EdamUri> annotations = new LinkedHashSet<>();
		Set<EdamUri> topicUris = edamUris(biotools.getTopics().stream(), concepts);
		if (topicUris != null) {
			annotations.addAll(topicUris);
		}
		Set<EdamUri> operationUris = edamUris(biotools.getFunctionNames().stream(), concepts);
		if (operationUris != null) {
			annotations.addAll(operationUris);
		}
		Set<EdamUri> dataUris = edamUris(biotools.getDataTypes().stream(), concepts);
		if (dataUris != null) {
			annotations.addAll(dataUris);
		}
		Set<EdamUri> formatUris = edamUris(biotools.getDataFormats().stream(), concepts);
		if (formatUris != null) {
			annotations.addAll(formatUris);
		}

		return new Query(
			null,
			biotools.getName().trim(),
			null,
			biotools.getDescription().trim(),
			webpageUrls,
			docUrls,
			publicationIds,
			annotations);
	}

	public static Query getBiotools(Tool tool, Map<EdamUri, Concept> concepts, int maxLinks, int maxPublicationIds, String filename) {
		List<Link> webpageUrls = new ArrayList<>();
		addLink(tool.getHomepage(), "Homepage", false, webpageUrls);
		if (tool.getLink() != null) {
			webpageUrls.addAll(linksJson(tool.getLink().stream(), Arrays.asList(
					LinkType.GALAXY_SERVICE,
					LinkType.MIRROR,
					LinkType.SOFTWARE_CATALOGUE,
					LinkType.REPOSITORY,
					LinkType.SERVICE,
					LinkType.OTHER,
					LinkType.BROWSER
				), false));
		}
		if (tool.getDownload() != null) {
			webpageUrls.addAll(downloadLinksJson(tool.getDownload().stream(), Arrays.asList(
					DownloadType.API_SPECIFICATION,
					DownloadType.BIOLOGICAL_DATA,
					DownloadType.COMMAND_LINE_SPECIFICATION,
					DownloadType.SOURCE_CODE,
					DownloadType.TEST_SCRIPT,
					DownloadType.TOOL_WRAPPER_CWL,
					DownloadType.DOWNLOADS_PAGE,
					DownloadType.OTHER
				), false));
		}

		List<Link> docUrls = new ArrayList<>();
		if (tool.getDocumentation() != null) {
			docUrls.addAll(linksJson(tool.getDocumentation().stream(), Arrays.asList(
					DocumentationType.API_DOCUMENTATION,
					DocumentationType.COMMAND_LINE_OPTIONS,
					DocumentationType.FAQ,
					DocumentationType.GENERAL,
					DocumentationType.INSTALLATION_INSTRUCTIONS,
					DocumentationType.QUICK_START_GUIDE,
					DocumentationType.TRAINING_MATERIAL,
					DocumentationType.USER_MANUAL,
					DocumentationType.OTHER
				), false));
		}

		List<PublicationIdsQuery> publicationIds = new ArrayList<>();
		if (tool.getPublication() != null) {
			for (Publication publication : tool.getPublication()) {
				addPublicationId(publication.getPmid(), publication.getPmcid(), publication.getDoi(), filename, publication.toStringType(), false, false, publicationIds);
			}
		}

		Set<EdamUri> annotations = new LinkedHashSet<>();
		if (tool.getTopic() != null) {
			annotations.addAll(edamUrisJson(tool.getTopic().stream(), concepts));
		}
		if (tool.getFunction() != null) {
			for (Function function : tool.getFunction()) {
				annotations.addAll(edamUrisJson(function.getOperation().stream(), concepts));
				if (function.getInput() != null) {
					for (InputOutput input : function.getInput()) {
						annotations.addAll(edamUrisJson(Collections.singletonList(input.getData()).stream(), concepts));
						if (input.getFormat() != null) {
							annotations.addAll(edamUrisJson(input.getFormat().stream(), concepts));
						}
					}
				}
				if (function.getOutput() != null) {
					for (InputOutput output : function.getOutput()) {
						annotations.addAll(edamUrisJson(Collections.singletonList(output.getData()).stream(), concepts));
						if (output.getFormat() != null) {
							annotations.addAll(edamUrisJson(output.getFormat().stream(), concepts));
						}
					}
				}
			}
		}

		if (maxLinks > 0 && webpageUrls.size() > maxLinks) {
			throw new IllegalRequestException("Number of links (" + webpageUrls.size() + ") is greater than maximum allowed (" + maxLinks + ")");
		}
		if (maxLinks > 0 && docUrls.size() > maxLinks) {
			throw new IllegalRequestException("Number of documentation links (" + docUrls.size() + ") is greater than maximum allowed (" + maxLinks + ")");
		}
		if (maxPublicationIds > 0 && publicationIds.size() > maxPublicationIds) {
			throw new IllegalRequestException("Number of publication IDs (" + publicationIds.size() + ") is greater than maximum allowed (" + maxPublicationIds + ")");
		}

		return new Query(
			tool.getBiotoolsID() != null ? tool.getBiotoolsID().trim() : null,
			tool.getName().trim(),
			null,
			tool.getDescription().trim(),
			webpageUrls,
			docUrls,
			publicationIds,
			annotations);
	}

	public static List<Query> get(String queryPath, QueryType type, Map<EdamUri, Concept> concepts, int timeout, String userAgent) throws IOException, ParseException {
		if (type == QueryType.server) {
			throw new IllegalArgumentException("Query of type \"" + QueryType.server.name() + "\" is not loadable from path, but has to be provided");
		}

		List<? extends InputType> inputs;
		if (type == QueryType.biotools) {
			inputs = Json.load(queryPath, type, timeout, userAgent);
		} else if (type == QueryType.biotools14) {
			inputs = Xml.load(queryPath, type, timeout, userAgent);
		} else {
			inputs = Csv.load(queryPath, type, timeout, userAgent);
		}

		Set<Query> queries = new LinkedHashSet<>();
		String filename = new File(queryPath).getName();

		for (InputType input : inputs) {
			switch (type) {
				case generic: queries.add(getGeneric((Generic) input, concepts, filename)); break;
				case SEQwiki: queries.add(getSEQwiki((SEQwiki) input, concepts, filename)); break;
				case msutils: queries.add(getMsutils((Msutils) input, concepts, filename)); break;
				case Bioconductor: queries.add(getBioconductor((Bioconductor) input, concepts)); break;
				case biotools14: queries.add(getBiotools14((Biotools14) input, concepts, filename)); break;
				case biotools: queries.add(getBiotools((Tool) input, concepts, 0, 0, filename)); break;
				case server: break;
			}
		}

		return new ArrayList<>(queries);
	}

	public static List<Query> get(String queryPath, QueryType type, int timeout, String userAgent) throws IOException, ParseException {
		return get(queryPath, type, null, timeout, userAgent);
	}

	private static Stream<String> parseServer(String string) {
		if (string == null) return null;
		return INTERNAL_SEPARATOR_NEWLINE.splitAsStream(string)
			.map(String::trim)
			.filter(s -> !s.isEmpty() && s.charAt(0) != '#');
	}
	private static List<List<String>> parseServerPublicationIds(String string) {
		if (string == null) return null;
		return INTERNAL_SEPARATOR_NEWLINE.splitAsStream(string)
			.filter(s -> !s.trim().isEmpty() && s.trim().charAt(0) != '#')
			.map(s -> PUBLICATION_ID_SEPARATOR.splitAsStream(s + " ").map(String::trim).collect(Collectors.toList()))
			.collect(Collectors.toList());
	}

	public static Query fromServer(ServerInput input, Map<EdamUri, Concept> concepts, int maxKeywords, int maxLinks, int maxPublicationIds) {
		try {
			input.check(1);
		} catch (ParseException e) {
			throw new IllegalRequestException(e);
		}
		return new Query(
			input.getId() != null ? input.getId().trim() : null,
			input.getName().trim(),
			keywords(parseServer(input.getKeywords()), "Keywords", null, maxKeywords),
			input.getDescription() != null ? input.getDescription().trim() : null,
			links(parseServer(input.getWebpageUrls()), null, true, maxLinks),
			links(parseServer(input.getDocUrls()), null, true, maxLinks),
			publicationIds(parseServerPublicationIds(input.getPublicationIds()), SERVER, null, true, true, maxPublicationIds),
			edamUris(parseServer(input.getAnnotations()), concepts));
	}

	public static List<Object> fromServerEntry(String input, DatabaseEntryType type, int max) {
		if (input == null) return Collections.emptyList();
		List<Object> list = Collections.emptyList();
		if (type == DatabaseEntryType.webpage || type == DatabaseEntryType.doc) {
			list = parseServer(input).map(s -> PubFetcher.getUrl(s, true)).collect(Collectors.toList());
		} else if (type == DatabaseEntryType.publication) {
			list = parseServerPublicationIds(input).stream().map(s -> {
				PublicationIds publicationIds;
				if (s.size() == 1) {
					publicationIds = PubFetcher.getPublicationIds(s.get(0), SERVER, true);
				} else if (s.size() == 3) {
					publicationIds = PubFetcher.getPublicationIds(s.get(0), s.get(1), s.get(2), SERVER, SERVER, SERVER, true, true);
				} else {
					throw new IllegalRequestException("Publication ID has illegal number of parts (" + s.size() + ")" + (s.size() > 0 ? ", first part is " + s.get(0) : ""));
				}
				return publicationIds;
			}).collect(Collectors.toList());
		} else {
			throw new IllegalArgumentException("Unknown database entry type: " + type);
		}
		if (max > 0 && list.size() > max) {
			throw new IllegalRequestException("Number of entries (" + list.size() + ") is greater than maximum allowed (" + max + ")");
		}
		return list;
	}

	public static Map<EdamUri, Concept> fromServerEdam(String input, Map<EdamUri, Concept> concepts) {
		if (input == null) return Collections.emptyMap();
		return edamUris(parseServer(input), concepts).stream()
			.collect(Collectors.toMap(e -> e, e -> concepts.get(e), (k, v) -> { throw new AssertionError(); }, LinkedHashMap::new));
	}
}
