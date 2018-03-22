package com.elastic.util;

import com.amazonaws.*;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.http.*;
import com.amazonaws.util.IOUtils;
import com.elastic.service.ProductServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.net.URI;

import static com.elastic.service.ProductService.INDEX_NAME;
import static com.elastic.service.ProductService.TYPE_NAME;

@Component
public class AmazonUtil {
    private static final String REGION = "";// region that your ES cluster is deployed to eg "us-west-2";
    private static final String ES_ENDPOINT = "";// endpoint of your ES cluster eg "http://blah-asdflasjfd.us-west-2.es.amazon.com"
    @Autowired
    AWSCredentials awsCredentials;

    /**
     * This method will commit in AWS elastic search
     * @param jsonPayload
     */
    public void doCommitInAWS(String jsonPayload) {
        Request<?> request = new DefaultRequest<Void>("es");
        request.setContent(new ByteArrayInputStream(jsonPayload.getBytes()));
        request.setEndpoint(URI.create(ES_ENDPOINT + "/" + INDEX_NAME + "/" + TYPE_NAME));
        request.setHttpMethod(HttpMethodName.POST);

        AWS4Signer signer = new AWS4Signer();
        signer.setRegionName("us-west-2");
        signer.setServiceName("es");
        signer.sign(request, awsCredentials);

        AmazonHttpClient client = new AmazonHttpClient(new ClientConfiguration());

        client.execute(request, new DummyHandler<>(new AmazonWebServiceResponse<Void>()), new DummyHandler<>(new AmazonServiceException("oops")), new ExecutionContext(true));


    }

    /**
     *
     * @param <T>
     */
    public static class DummyHandler<T> implements HttpResponseHandler<T> {
        private final T preCannedResponse;
        public DummyHandler(T preCannedResponse) { this.preCannedResponse = preCannedResponse; }

        @Override
        public T handle(HttpResponse response) throws Exception {
            System.out.println(IOUtils.toString(response.getContent()));
            return preCannedResponse;
        }

        @Override
        public boolean needsConnectionLeftOpen() { return false; }
    }

}
