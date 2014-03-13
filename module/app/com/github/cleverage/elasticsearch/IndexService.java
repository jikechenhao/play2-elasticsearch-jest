package com.github.cleverage.elasticsearch;

import io.searchbox.client.JestResult;
import io.searchbox.core.Delete;
import io.searchbox.core.Percolate;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.Flush;
import io.searchbox.indices.Refresh;
import io.searchbox.indices.mapping.PutMapping;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.index.percolator.PercolatorService;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.indices.IndexMissingException;
import play.Logger;
import play.libs.F;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.github.cleverage.elasticsearch.JestClientConfig.jestXcute;


public abstract class IndexService {

    public static final String INDEX_DEFAULT = IndexClient.config.indexNames[0];
    public static final String INDEX_PERCOLATOR = PercolatorService.INDEX_NAME;

    /**
     * get indexRequest to index from a specific request
     *
     * @return
     */
    public static JestIndexRequestBuilder getIndexRequest(IndexQueryPath indexPath, String id, Index indexable) {
        return (JestIndexRequestBuilder) new JestIndexRequestBuilder(indexPath.index)
                .setType(indexPath.type)
                .setId(id)
                .setSource(indexable.toIndex());
    }

    /**
     * index from an request
     *
     * @param requestBuilder
     * @return
     */
    public static JestResult index(JestIndexRequestBuilder requestBuilder) {

        JestResult jestResult = requestBuilder.jestXcute();

        if (Logger.isDebugEnabled()) {
            Logger.debug("ElasticSearch : Index " + jestResult.getJsonString());
        }
        return jestResult;
    }

    /**
     * Create an JestIndexRequestBuilder
     * @param indexPath
     * @param id
     * @param indexable
     * @return
     */
    private static JestIndexRequestBuilder getJestIndexRequestBuilder(IndexQueryPath indexPath, String id, Index indexable) {
        return getIndexRequest(indexPath, id, indexable);
    }

    /**
     * Add Indexable object in the index
     *
     * @param indexPath
     * @param indexable
     * @return
     */
    public static JestResult index(IndexQueryPath indexPath, String id, Index indexable) {
        JestResult jestResult = getJestIndexRequestBuilder(indexPath, id, indexable).jestXcute();
        if (Logger.isDebugEnabled()) {
            Logger.debug("ElasticSearch : Index : " + jestResult.getJsonString());
        }
        return jestResult;
    }

    /**
     * Add Indexable object in the index asynchronously
     *
     * @param indexPath
     * @param indexable
     * @return
     */
    public static F.Promise<JestResult> indexAsync(IndexQueryPath indexPath, String id, Index indexable) {
        return indexAsync(getJestIndexRequestBuilder(indexPath, id, indexable));
    }

    /**
     * call JestIndexRequestBuilder on asynchronously
     * @param jestIndexRequestBuilder
     * @return
     */
    public static F.Promise<JestResult> indexAsync(JestIndexRequestBuilder jestIndexRequestBuilder) {
        return null;//F.Promise.wrap(AsyncUtils.createPromise().success(jestIndexRequestBuilder.jestXcute()).future());
    }

    /**
     * Add a json document to the index
     * @param indexPath
     * @param id
     * @param json
     * @return
     */
    public static JestResult index(IndexQueryPath indexPath, String id, String json) {
        return getJestIndexRequestBuilder(indexPath, id, json).jestXcute();
    }

    /**
     * Create an JestIndexRequestBuilder for a Json-encoded object
     * @param indexPath
     * @param id
     * @param json
     * @return
     */
    public static JestIndexRequestBuilder getJestIndexRequestBuilder(IndexQueryPath indexPath, String id, String json) {
        return (JestIndexRequestBuilder) new JestIndexRequestBuilder(indexPath.index)
                .setType(indexPath.type)
                .setId(id)
                .setSource(json);
    }

    /**
     * Create a BulkRequestBuilder for a List of Index objects
     * @param indexPath
     * @param indexables
     * @return
     */
    private static JestBulkRequestBuilder getBulkRequestBuilder(IndexQueryPath indexPath, List<? extends Index> indexables) {
        final JestBulkRequestBuilder jestBulkRequestBuilder = new JestBulkRequestBuilder();
        for (Index indexable : indexables) {
            jestBulkRequestBuilder.add(getJestIndexRequestBuilder(indexPath, indexable.id, indexable).getAction());
        }
        return jestBulkRequestBuilder;
    }

    /**
     * Bulk index a list of indexables
     * @param indexPath
     * @param indexables
     * @return
     */
    public static JestResult indexBulk(IndexQueryPath indexPath, List<? extends Index> indexables) {
        JestBulkRequestBuilder bulkRequestBuilder = getBulkRequestBuilder(indexPath, indexables);
        return bulkRequestBuilder.jestXcute();
    }

    /**
     * Bulk index a list of indexables asynchronously
     * @param indexPath
     * @param indexables
     * @return
     */
    public static F.Promise<JestResult> indexBulkAsync(IndexQueryPath indexPath, List<? extends Index> indexables) {
//        return AsyncUtils.executeAsyncJava(getBulkRequestBuilder(indexPath, indexables));
        return null;
    }

    /**
     * Create a BulkRequestBuilder for a List of json-encoded objects
     * @param indexPath
     * @param jsonMap
     * @return
     */
    public static JestBulkRequestBuilder getBulkRequestBuilder(IndexQueryPath indexPath, Map<String, String> jsonMap) {
        JestBulkRequestBuilder bulkRequestBuilder = new JestBulkRequestBuilder();
        for (String id : jsonMap.keySet()) {
            bulkRequestBuilder.add(getJestIndexRequestBuilder(indexPath, id, jsonMap.get(id)));
        }
        return bulkRequestBuilder;
    }

    /**
     * Bulk index a list of indexables asynchronously
     * @param bulkRequestBuilder
     * @return
     */
    public static F.Promise<BulkResponse> indexBulkAsync(BulkRequestBuilder bulkRequestBuilder) {
//        return AsyncUtils.executeAsyncJava(bulkRequestBuilder);
        return null;
    }

    /**
     * Create a BulkRequestBuilder for a List of JestIndexRequestBuilder
     * @return
     */
    public static JestBulkRequestBuilder getBulkRequestBuilder(Collection<JestIndexRequestBuilder> JestIndexRequestBuilder) {
        JestBulkRequestBuilder bulkRequestBuilder = new JestBulkRequestBuilder();
        for (JestIndexRequestBuilder requestBuilder : JestIndexRequestBuilder) {
            bulkRequestBuilder.add(requestBuilder);
        }
        return bulkRequestBuilder;
    }

    /**
     * Bulk index a Map of json documents.
     * The id of the document is the key of the Map
     * @param indexPath
     * @param jsonMap
     * @return
     */
    public static JestResult indexBulk(IndexQueryPath indexPath, Map<String, String> jsonMap) {
        JestBulkRequestBuilder bulkRequestBuilder = getBulkRequestBuilder(indexPath, jsonMap);
        return bulkRequestBuilder.jestXcute();
    }

    /**
     * Create an UpdateRequestBuilder
     * @param indexPath
     * @param id
     * @return
     */
    public static JestUpdateRequestBuilder getUpdateRequestBuilder(IndexQueryPath indexPath,
                                                                   String id,
                                                                   Map<String, Object> updateFieldValues,
                                                                   String updateScript) {
        return new JestUpdateRequestBuilder(indexPath.index, indexPath.type, id).setScriptParams(updateFieldValues).setScript(updateScript);
    }

    /**
     * Update a document in the index
     * @param indexPath
     * @param id
     * @param updateFieldValues The fields and new values for which the update should be done
     * @param updateScript
     * @return
     */
    public static JestResult update(IndexQueryPath indexPath,
                                    String id,
                                    Map<String, Object> updateFieldValues,
                                    String updateScript) {
        return getUpdateRequestBuilder(indexPath, id, updateFieldValues, updateScript)
                .jestXcute();
    }

    /**
     * Update a document asynchronously
     * @param indexPath
     * @param id
     * @param updateFieldValues The fields and new values for which the update should be done
     * @param updateScript
     * @return
     */
    public static F.Promise<UpdateResponse> updateAsync(IndexQueryPath indexPath,
                                                        String id,
                                                        Map<String, Object> updateFieldValues,
                                                        String updateScript) {
//        return updateAsync(getUpdateRequestBuilder(indexPath, id, updateFieldValues, updateScript));
        return null;
    }

    /**
     * Call update asynchronously
     * @param updateRequestBuilder
     * @return
     */
    public static F.Promise<UpdateResponse> updateAsync(UpdateRequestBuilder updateRequestBuilder) {
        return AsyncUtils.executeAsyncJava(updateRequestBuilder);
    }

    /**
     * Create a DeleteRequestBuilder
     * @param indexPath
     * @param id
     * @return
     */
    public static JestDeleteRequestBuilder getDeleteRequestBuilder(IndexQueryPath indexPath, String id) {
        return new JestDeleteRequestBuilder(indexPath.index, indexPath.type, id);
    }

    /**
     * Delete element in index asynchronously
     * @param indexPath
     * @return
     */
    public static F.Promise<DeleteResponse> deleteAsync(IndexQueryPath indexPath, String id) {
//        return AsyncUtils.executeAsyncJava(getDeleteRequestBuilder(indexPath, id));
        return null;
    }

    /**
     * Delete element in index
     * @param indexPath
     * @return
     */
    public static JestResult delete(IndexQueryPath indexPath, String id) {
        JestResult deleteResponse = getDeleteRequestBuilder(indexPath, id)
                .jestXcute();

        if (Logger.isDebugEnabled()) {
            Logger.debug("ElasticSearch : Delete " + deleteResponse.toString());
        }

        return deleteResponse;
    }

    /**
     * Create a GetRequestBuilder
     * @param indexPath
     * @param id
     * @return
     */
    public static JestGetRequestBuilder getGetRequestBuilder(IndexQueryPath indexPath, String id) {
        return new JestGetRequestBuilder(indexPath.index, indexPath.type, id);
    }

    /**
     * Get the json representation of a document from an id
     * @param indexPath
     * @param id
     * @return
     */
    public static String getAsString(IndexQueryPath indexPath, String id) {
        return getGetRequestBuilder(indexPath, id)
                .jestXcute().getJsonString();
    }

    private static <T extends Index> T getTFromGetResponse(Class<T> clazz, GetResponse getResponse) {
        T t = IndexUtils.getInstanceIndex(clazz);
        if (!getResponse.isExists()) {
            return null;
        }

        // Create a new Indexable Object for the return
        Map<String, Object> map = getResponse.getSourceAsMap();

        t = (T) t.fromIndex(map);
        t.id = getResponse.getId();
        return t;
    }

    /**
     * Get Indexable Object for an Id
     *
     * @param indexPath
     * @param clazz
     * @return
     */
    public static <T extends Index> T get(IndexQueryPath indexPath, Class<T> clazz, String id) {
        JestGetRequestBuilder getRequestBuilder = getGetRequestBuilder(indexPath, id);
        return getRequestBuilder.jestXcute().getSourceAsObject(clazz);
    }

    /**
     * Get Indexable Object for an Id asynchronously
     * @param indexPath
     * @param clazz
     * @param id
     * @param <T>
     * @return
     */
    public static <T extends Index> F.Promise<T> getAsync(IndexQueryPath indexPath, final Class<T> clazz, String id) {
//        F.Promise<GetResponse> responsePromise = AsyncUtils.executeAsyncJava(getGetRequestBuilder(indexPath, id));
//        return responsePromise.map(
//            new F.Function<GetResponse, T>() {
//                public T apply(GetResponse getResponse) {
//                    return getTFromGetResponse(clazz, getResponse);
//                }
//            }
//        );
        return null;
    }

    /**
     * Get a reponse for a simple request
     * @param indexName
     * @param indexType
     * @param id
     * @return
     */
    public static JestResult get(String indexName, String indexType, String id) {
        return new JestGetRequestBuilder(indexName, indexType, id).jestXcute();
    }

    /**
     * Search information on Index from a query
     * @param indexQuery
     * @param <T>
     * @return
     */
    public static <T extends Index> IndexResults<T> search(IndexQueryPath indexPath, IndexQuery<T> indexQuery) {
        return indexQuery.fetch(indexPath);
    }

    /**
     * Search asynchronously information on Index from a query
     * @param indexPath
     * @param indexQuery
     * @param <T>
     * @return
     */
    public static <T extends Index> F.Promise<IndexResults<T>> searchAsync(IndexQueryPath indexPath,
                                                                           IndexQuery<T> indexQuery,
                                                                           FilterBuilder filter) {
        return indexQuery.fetchAsync(indexPath, filter);
    }

    /**
     * Test if an indice Exists
     * @return true if exists
     */
    public static boolean existsIndex(String indexName) {

//        Client client = IndexClient.client;
//        AdminClient admin = client.admin();
//        IndicesAdminClient indices = admin.indices();
//        IndicesExistsRequestBuilder indicesExistsRequestBuilder = indices.prepareExists(indexName);
//        IndicesExistsResponse response = indicesExistsRequestBuilder.execute().actionGet();
//
//        return response.isExists();
        return false;
    }

    /**
     * Create the index
     */
    public static void createIndex(String indexName) {
//        Logger.debug("ElasticSearch : creating index [" + indexName + "]");
//        try {
//            CreateJestIndexRequestBuilder creator = IndexClient.client.admin().indices().prepareCreate(indexName);
//            String setting = IndexClient.config.indexSettings.get(indexName);
//            if (setting != null) {
//                creator.setSettings(setting);
//            }
//            creator.execute().actionGet();
//        } catch (Exception e) {
//            Logger.error("ElasticSearch : Index create error : " + e.toString());
//        }
    }

    /**
     * Delete the index
     */
    public static void deleteIndex(String indexName) {
//        Logger.debug("ElasticSearch : deleting index [" + indexName + "]");
//        try {
//            IndexClient.client.admin().indices().prepareDelete(indexName).execute().actionGet();
//        } catch (IndexMissingException indexMissing) {
//            Logger.debug("ElasticSearch : Index " + indexName + " no exists");
//        } catch (Exception e) {
//            Logger.error("ElasticSearch : Index drop error : " + e.toString());
//        }
    }

    /**
     * Create Mapping ( for example mapping type : nested, geo_point  )
     * see http://www.elasticsearch.org/guide/reference/mapping/
     * <p/>
     * {
     * "tweet" : {
     * "properties" : {
     * "message" : {"type" : "string", "store" : "yes"}
     * }
     * }
     * }
     *  @param indexName
     * @param indexType
     * @param indexMapping
     */
    public static JestResult createMapping(String indexName, String indexType, String indexMapping) {
        Logger.debug("ElasticSearch : creating mapping [" + indexName + "/" + indexType + "] :  " + indexMapping);
        final PutMapping build = new PutMapping.Builder(indexName, indexType, indexMapping).build();
        return jestXcute(build);
    }

    /**
     * Read the Mapping for a type
     * @param indexType
     * @return
     */
    public static String getMapping(String indexName, String indexType) {
        return null;
//        ClusterState state = IndexClient.client.admin().cluster()
//                .prepareState()
//                .setFilterIndices(IndexService.INDEX_DEFAULT)
//                .execute().actionGet().getState();
//        MappingMetaData mappingMetaData = state.getMetaData().index(indexName).mapping(indexType);
//        if (mappingMetaData != null) {
//            return mappingMetaData.source().toString();
//        } else {
//            return null;
//        }
    }

    /**
     * call createMapping for list of @indexType
     * @param indexName
     */
    public static void prepareIndex(String indexName) {

        Map<IndexQueryPath, String> indexMappings = IndexClient.config.indexMappings;
        for (IndexQueryPath indexQueryPath : indexMappings.keySet()) {

            if(indexName != null && indexName.equals(indexQueryPath.index)) {
                String indexType = indexQueryPath.type;
                String indexMapping = indexMappings.get(indexQueryPath);
                if (indexMapping != null) {
                    createMapping(indexName, indexType, indexMapping);
                }
            }
        }
    }

    public static void cleanIndex() {

        String[] indexNames = IndexClient.config.indexNames;
        for (String indexName : indexNames) {
            cleanIndex(indexName);
        }
    }

    public static void cleanIndex(String indexName) {

        if (IndexService.existsIndex(indexName)) {
            IndexService.deleteIndex(indexName);
        }
        IndexService.createIndex(indexName);
        IndexService.prepareIndex(indexName);
    }

    /**
     * Refresh full index
     */
    public static void refresh() {
        String[] indexNames = IndexClient.config.indexNames;
        for (String indexName : indexNames) {
            refresh(indexName);
        }
    }

    /**
     * Refresh an index
     * @param indexName
     */
    private static void refresh(String indexName) {
        jestXcute(new Refresh.Builder().addIndex(indexName).build());
    }

    /**
     * Flush full index
     */
    public static void flush() {
        String[] indexNames = IndexClient.config.indexNames;
        for (String indexName : indexNames) {
            flush(indexName);
        }
    }

    /**
     * Flush an index
     * @param indexName
     */
    public static void flush(String indexName) {
        jestXcute(new Flush.Builder().addIndex(indexName));
    }

    /**
     * Create Percolator from a queryBuilder
     *
     * @param namePercolator
     * @param queryBuilder
     * @return
     * @throws IOException
     */
    public static JestResult createPercolator(String namePercolator, QueryBuilder queryBuilder) {
        //TODO check arguments > add name as id
        return jestXcute(new Percolate.Builder(INDEX_PERCOLATOR, INDEX_DEFAULT, queryBuilder).build());
    }

    /**
     * Create Percolator
     *
     * @param namePercolator
     * @param query
     * @return
     * @throws IOException
     */
    public static JestResult createPercolator(String namePercolator, String query) {
        return createPercolator(namePercolator, new QueryStringQueryBuilder(query));
    }

    /**
     * Check if a percolator exists
     * @param namePercolator
     * @return
     */
    public static boolean precolatorExists(String namePercolator) {
        try {
            JestResult responseExist = IndexService.getPercolator(namePercolator);
            return (responseExist.isSucceeded());
        } catch (IndexMissingException e) {
            return false;
        }
    }

    /**
     * Delete Percolator
     *
     * @param namePercolator
     * @return
     */
    public static JestResult deletePercolator(String namePercolator) {
        return delete(new IndexQueryPath(INDEX_PERCOLATOR, IndexService.INDEX_DEFAULT), namePercolator);
    }

    /**
     * Delete all percolators
     */
    public static void deletePercolators() {
        try {
            final JestResult jestResult = jestXcute(new DeleteIndex.Builder(INDEX_PERCOLATOR).build());
            if(!jestResult.isSucceeded()){
                throw new Exception(" no acknowledged");
            }
        } catch (IndexMissingException indexMissing) {
            Logger.debug("ElasticSearch : Index " + INDEX_PERCOLATOR + " no exists");
        } catch (Exception e) {
            Logger.error("ElasticSearch : Index drop error : " + e.toString());
        }
    }

    /**
     * Get the percolator details
     * @param name
     * @return
     */
    public static JestResult getPercolator(String name) {
        return get(INDEX_PERCOLATOR, IndexService.INDEX_DEFAULT, name);
    }

    /**
     * Get percolator match this Object
     *
     * @param indexable
     * @return
     * @throws IOException
     */
    public static List<String> getPercolatorsForDoc(Index indexable) {

//        PercolateRequestBuilder percolateRequestBuilder = new PercolateRequestBuilder(IndexClient.client, indexable.getIndexPath().index, indexable.getIndexPath().type);
//
//        XContentBuilder doc = null;
//        try {
//            doc = jsonBuilder().startObject().startObject("doc").startObject(indexable.getIndexPath().type);
//            Map<String, Object> map = indexable.toIndex();
//            for (String key : map.keySet()) {
//                if (key != null && map.get(key) != null) {
//                    doc.field(key, map.get(key));
//                }
//            }
//            doc.endObject().endObject().endObject();
//        } catch (Exception e) {
//            Logger.debug("Elasticsearch : Error when get percolator for ");
//        }
//
//        percolateRequestBuilder.setSource(doc);
//
//        PercolateResponse percolateResponse = percolateRequestBuilder.execute().actionGet();
//        if (percolateResponse == null) {
//            return null;
//        }
//        return percolateResponse.getMatches();
        return null;
    }
}
