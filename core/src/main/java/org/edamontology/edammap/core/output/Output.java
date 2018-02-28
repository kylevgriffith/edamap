/*
 * Copyright © 2016, 2017 Erik Jaaniso
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

package org.edamontology.edammap.core.output;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.edamontology.edammap.core.args.MainArgs;
import org.edamontology.edammap.core.edam.Concept;
import org.edamontology.edammap.core.edam.EdamUri;
import org.edamontology.edammap.core.mapping.Mapping;
import org.edamontology.edammap.core.query.Query;
import org.edamontology.pubfetcher.FetcherCommon;
import org.edamontology.pubfetcher.Publication;

public class Output {

	private MainArgs args;

	private Path output;

	private Path report;

	public Output(MainArgs args) throws IOException {
		this.args = args;

		this.output = FetcherCommon.outputPath(args.getOutput(), true);

		this.report = FetcherCommon.outputPath(args.getReport(), true);
	}

	public void output(Map<EdamUri, Concept> concepts, List<Query> queries, List<List<Publication>> publications, List<Mapping> mappings) throws IOException {
		Txt.output(args.getType(), output, concepts, queries, publications, mappings);
		Report.output(args, report, concepts, queries, publications, mappings);
	}
}