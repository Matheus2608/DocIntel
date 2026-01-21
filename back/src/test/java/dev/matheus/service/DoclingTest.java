package dev.matheus.service;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.request.options.TableFormerMode;
import ai.docling.serve.api.convert.request.source.HttpSource;
import ai.docling.serve.api.convert.request.target.InBodyTarget;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.URI;

@QuarkusTest
public class DoclingTest {

    @Inject
    DoclingServeApi doclingServeApi;

    @Test
    void testDocling() {

        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .source(HttpSource.builder().url(URI.create("https://arxiv.org/pdf/2408.09869"))
                        .build())
                .options(ConvertDocumentOptions.builder()
                        .toFormat(OutputFormat.MARKDOWN) // request Markdown output
                        .includeImages(true)
                        .build())
                .target(InBodyTarget.builder().build()) // get results in the HTTP response body
                .build();

        ConvertDocumentResponse response = doclingServeApi
                .convertSource(request);
        System.out.println(response.getDocument().getMarkdownContent());

//        var sla = doclingServeApi.convertFiles(
//                ConvertDocumentRequest.builder()
//                        .options(ConvertDocumentOptions.builder()
//                                .includeImages(false)
//                                .abortOnError(false)
//                                .doPictureClassification(false)
//                                .doFormulaEnrichment(true)
//                                .tableMode(TableFormerMode.ACCURATE)
//                                .tableCellMatching(true)
//                                .
//                                .build())
//                        .source()
//                        .target()
//                        .
//                        .build());
    }
}
