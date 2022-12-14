package com.allezon.aerospike.schema;

import com.allezon.aerospike.UserProfile;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.schema.registry.SchemaReference;
import org.springframework.cloud.schema.registry.SchemaRegistrationResponse;
import org.springframework.cloud.schema.registry.client.SchemaRegistryClient;
import org.springframework.stereotype.Component;

@Component
public class SchemaVersion implements InitializingBean {

    @Autowired
    private SchemaRegistryClient schemaRegistryClient;

    private SchemaReference currentSchemaReference;

    public int getCurrentSchemaVersion() {
        return currentSchemaReference.getVersion();
    }

    @Override
    public void afterPropertiesSet() {
//        SchemaRegistrationResponse register = schemaRegistryClient.register("Message", "avro", Message.getClassSchema().toString());
        SchemaRegistrationResponse register = schemaRegistryClient.register("UserProfile", "avro", UserProfile.getClassSchema().toString());
        // todo sprawdzic czy ma sens (czy nie dodac innych pól)
        this.currentSchemaReference = register.getSchemaReference();
    }

}
