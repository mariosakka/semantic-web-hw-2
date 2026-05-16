package com.example.semanticweb.service;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.springframework.stereotype.Service;

@Service
public class GraphRenderService {

    public String renderModelToSvg(Model model) {
        try {
            return Graphviz.fromString(toDot(model)).render(Format.SVG).toString();
        } catch (Exception e) {
            throw new IllegalStateException("Could not render RDF graph", e);
        }
    }

    private String toDot(Model model) {
        StringBuilder builder = new StringBuilder();
        builder.append("digraph RDF {\n");
        builder.append("rankdir=LR;\n");
        builder.append("node [shape=box, fontsize=10];\n");

        StmtIterator statements = model.listStatements();
        while (statements.hasNext()) {
            Statement statement = statements.next();
            String subject = shortResource(statement.getSubject().toString());
            String predicate = statement.getPredicate().getLocalName() == null
                    ? statement.getPredicate().toString()
                    : statement.getPredicate().getLocalName();

            RDFNode objectNode = statement.getObject();
            String object = objectNode.isResource()
                    ? shortResource(objectNode.asResource().toString())
                    : objectNode.asLiteral().getString();

            builder.append(quoted(subject))
                    .append(" -> ")
                    .append(quoted(object))
                    .append(" [label=")
                    .append(quoted(predicate))
                    .append("];\n");
        }

        builder.append("}\n");
        return builder.toString();
    }

    private String shortResource(String value) {
        int hashIndex = value.lastIndexOf('#');
        if (hashIndex >= 0 && hashIndex + 1 < value.length()) {
            return value.substring(hashIndex + 1);
        }
        int slashIndex = value.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex + 1 < value.length()) {
            return value.substring(slashIndex + 1);
        }
        return value;
    }

    private String quoted(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");
        return "\"" + escaped + "\"";
    }
}
