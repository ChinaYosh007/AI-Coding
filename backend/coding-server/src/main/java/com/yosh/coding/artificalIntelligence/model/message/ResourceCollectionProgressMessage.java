package com.yosh.coding.artificalIntelligence.model.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Sent before code generation so the client can display resource-collection progress.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ResourceCollectionProgressMessage extends StreamMessage {

    private String data;

    public ResourceCollectionProgressMessage(String data) {
        super("resource_collection_progress");
        this.data = data;
    }
}
