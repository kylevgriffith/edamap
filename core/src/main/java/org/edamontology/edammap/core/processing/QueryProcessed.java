/*
 * Copyright © 2016, 2018, 2019 Erik Jaaniso
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

package org.edamontology.edammap.core.processing;

import java.util.ArrayList;
import java.util.List;

import org.edamontology.pubfetcher.core.db.publication.Publication;
import org.edamontology.pubfetcher.core.db.webpage.Webpage;

public class QueryProcessed {

	private List<String> nameTokens = null;
	private List<Double> nameIdfs = null;

	private List<List<String>> keywordsTokens = new ArrayList<>();
	private List<List<Double>> keywordsIdfs = new ArrayList<>();

	private List<List<String>> descriptionTokens = new ArrayList<>();
	private List<List<Double>> descriptionIdfs = null;

	private List<Webpage> webpages = new ArrayList<>();
	private List<List<List<String>>> webpagesTokens = new ArrayList<>();
	private List<List<List<Double>>> webpagesIdfs = new ArrayList<>();

	private List<Webpage> docs = new ArrayList<>();
	private List<List<List<String>>> docsTokens = new ArrayList<>();
	private List<List<List<Double>>> docsIdfs = new ArrayList<>();

	private List<Publication> publications = new ArrayList<>();
	private List<PublicationProcessed> processedPublications = new ArrayList<>();

	public List<String> getNameTokens() {
		return nameTokens;
	}
	public void setNameTokens(List<String> nameTokens) {
		this.nameTokens = nameTokens;
	}
	public List<Double> getNameIdfs() {
		return nameIdfs;
	}
	public void setNameIdfs(List<Double> nameIdfs) {
		this.nameIdfs = nameIdfs;
	}

	public List<List<String>> getKeywordsTokens() {
		return keywordsTokens;
	}
	public void addKeywordTokens(List<String> keywordTokens) {
		this.keywordsTokens.add(keywordTokens);
	}
	public List<List<Double>> getKeywordsIdfs() {
		return keywordsIdfs;
	}
	public void addKeywordIdfs(List<Double> keywordIdfs) {
		this.keywordsIdfs.add(keywordIdfs);
	}

	public List<List<String>> getDescriptionTokens() {
		return descriptionTokens;
	}
	public void addDescriptionTokens(List<String> descriptionTokens) {
		this.descriptionTokens.add(descriptionTokens);
	}
	public List<List<Double>> getDescriptionIdfs() {
		return descriptionIdfs;
	}
	public void addDescriptionIdfs(List<Double> descriptionIdfs) {
		if (this.descriptionIdfs == null) {
			this.descriptionIdfs = new ArrayList<>();
		}
		this.descriptionIdfs.add(descriptionIdfs);
	}

	public List<Webpage> getWebpages() {
		return webpages;
	}
	public void addWebpage(Webpage webpage) {
		this.webpages.add(webpage);
	}
	public List<List<List<String>>> getWebpagesTokens() {
		return webpagesTokens;
	}
	public void addWebpageTokens(List<List<String>> webpageTokens) {
		this.webpagesTokens.add(webpageTokens);
	}
	public List<List<List<Double>>> getWebpagesIdfs() {
		return webpagesIdfs;
	}
	public void addWebpageIdfs(List<List<Double>> webpageIdfs) {
		this.webpagesIdfs.add(webpageIdfs);
	}

	public List<Webpage> getDocs() {
		return docs;
	}
	public void addDoc(Webpage doc) {
		this.docs.add(doc);
	}
	public List<List<List<String>>> getDocsTokens() {
		return docsTokens;
	}
	public void addDocTokens(List<List<String>> docTokens) {
		this.docsTokens.add(docTokens);
	}
	public List<List<List<Double>>> getDocsIdfs() {
		return docsIdfs;
	}
	public void addDocIdfs(List<List<Double>> docIdfs) {
		this.docsIdfs.add(docIdfs);
	}

	public List<Publication> getPublications() {
		return publications;
	}
	public void addPublication(Publication publication) {
		this.publications.add(publication);
	}
	public List<PublicationProcessed> getProcessedPublications() {
		return processedPublications;
	}
	public void addProcessedPublication(PublicationProcessed processedPublication) {
		this.processedPublications.add(processedPublication);
	}
}
