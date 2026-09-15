package com.card.client.rag;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Component
public class CardProgramsLoader {

    private final VectorStore vectorStore;

    @Value("classpath:card_products/*.pdf")
    private Resource[] programs;

    public CardProgramsLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }


    @PostConstruct
    public void loadCardsPDFs() {

        Arrays.stream(programs)
                .forEach( program ->{
                            TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(program);
                            List<Document> docs = tikaDocumentReader.get();
                            TextSplitter textSplitter =
                                    TokenTextSplitter.builder().withChunkSize(200).build();
                              //vectorStore.add(textSplitter.split(docs));
                        }
           );


    }

}
