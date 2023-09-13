package io.jenkins.plugins.dogu.api;

import com.google.gson.Gson;
import io.jenkins.plugins.dogu.DoguOption;
import io.jenkins.plugins.dogu.api.DoguApiResponse.RunRoutineResponse;
import io.jenkins.plugins.dogu.api.DoguApiResponse.UploadApplicationResponse;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class DoguApiClient {
    public static DoguWebSocketClient connectRoutine(
            PrintStream logger, String projectId, String routineId, int routinePipelineId, DoguOption option)
            throws Exception {
        String url = option.getWebSocketUrl(logger) + "/v1/pipeline-state?projectId=" + projectId + "&routineId="
                + routineId + "&pipelineId=" + routinePipelineId;

        URI uri = new URI(url);
        DoguWebSocketClient client = new DoguWebSocketClient(uri, logger);
        client.addHeader("Authorization", "Bearer " + option.DOGU_TOKEN.getPlainText());
        client.connect();

        return client;
    }

    public static RunRoutineResponse runRoutine(String projectId, String routineId, DoguOption option)
            throws Exception {
        String url = option.API_URL + "/v1/projects/" + projectId + "/routines/" + routineId + "/pipelines";
        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw e;
        }

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(uri)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + option.DOGU_TOKEN.getPlainText())
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw e;
        }

        int status = response.statusCode();
        if (status < 200 || status >= 400) {
            throw new Exception("Failed to run routine: " + status + "\n" + response.body());
        }

        RunRoutineResponse runRoutineResponse = new Gson().fromJson(response.body(), RunRoutineResponse.class);

        return runRoutineResponse;
    }

    public static UploadApplicationResponse uploadApplication(
            String projectId, byte[] fileContent, String fileName, String mimeType, Boolean isLatest, DoguOption option)
            throws Exception {
        String url = option.API_URL + "/v1/projects/" + projectId + "/applications";
        URI uri;

        System.out.println("API client: " + isLatest);

        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw e;
        }

        String boundary = UUID.randomUUID().toString();
        String boundaryLine = "--" + boundary;
        String endBoundaryLine = boundaryLine + "--";
        String contentType = "multipart/form-data; boundary=" + boundary;
        String isLatestString = isLatest.toString();

        String requestBody = boundaryLine + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                + fileName + "\"" + "\r\nContent-Type: application/vnd.android.package-archive"
                + "\r\n\r\n" + boundaryLine + "\r\nContent-Disposition: form-data; name=\"isLatest\";"
                + isLatestString + "\"" + "\r\nContent-Type: text/plain" + "\r\n\r\n";

        byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
        byte[] endBoundaryBytes = ("\r\n" + endBoundaryLine).getBytes(StandardCharsets.UTF_8);

        byte[] payload = new byte[requestBodyBytes.length + fileContent.length + endBoundaryBytes.length];
        System.arraycopy(requestBodyBytes, 0, payload, 0, requestBodyBytes.length);
        System.arraycopy(fileContent, 0, payload, requestBodyBytes.length, fileContent.length);
        System.arraycopy(
                endBoundaryBytes, 0, payload, requestBodyBytes.length + fileContent.length, endBoundaryBytes.length);

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(uri)
                .header("Content-Type", contentType)
                .header("Authorization", "Bearer " + option.DOGU_TOKEN.getPlainText())
                .PUT(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw e;
        }

        int status = response.statusCode();
        if (status < 200 || status >= 400) {
            throw new Exception("Failed to upload application: " + status + "\n" + response.body());
        }

        UploadApplicationResponse uploadApplicationResponse =
                new Gson().fromJson(response.body(), UploadApplicationResponse.class);

        return uploadApplicationResponse;
    }
}
