package io.donbee.jade.rest.handler;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VersionHandlerTest {

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    private VersionHandler handler;

    @Before
    public void setUp() {
        handler = new VersionHandler();
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockResponse.setStatusCode(200)).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
    }

    @Test
    public void Given_Request_When_GetVersion_Then_Returns200WithVersionFields() {
        // Arrange

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).putHeader("Content-Type", "application/json");
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_VersionManagerAvailable_When_GetVersion_Then_ResponseContainsVersionAndRevisionAndDate() {
        // Arrange

        // Act
        handler.handle(mockContext);

        // Assert
        // Verify response ends with a buffer that contains version, revision, date fields
        // The VersionHandler creates a VersionManager internally; even if it succeeds or fails,
        // the response is always 200 with those three fields (UNKNOWN is fallback).
        verify(mockResponse).end(argThat((Buffer buf) -> {
            String json = buf.toString();
            return json.contains("version") && json.contains("revision") && json.contains("date");
        }));
    }
}
