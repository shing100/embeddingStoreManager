package com.kingname.embeddingstoremanager.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;


@Getter
@Setter
@SuperBuilder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EsCachedEmbeddingDocument extends CachedEmbeddingDocument{
    private String id;
    private String hash;
    private Boolean has_embedding;
}
