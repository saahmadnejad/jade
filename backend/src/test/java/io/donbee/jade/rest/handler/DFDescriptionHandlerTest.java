package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.DFService;
import io.donbee.jade.rest.service.DFService.DFRegistrationInfo;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DFDescriptionHandlerTest {

    @Mock
    private DFService mockService;

    @Mock
    private RoutingContext mockContext;

    @Mock
    private HttpServerResponse mockResponse;

    @Before
    public void setUp() {
        when(mockContext.response()).thenReturn(mockResponse);
        when(mockResponse.setStatusCode(anyInt())).thenReturn(mockResponse);
        when(mockResponse.putHeader(anyString(), anyString())).thenReturn(mockResponse);
        doAnswer(inv -> mockResponse).when(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_DFDescriptionAvailable_When_GetDescription_Then_Returns200() {
        // Arrange
        DFDescriptionHandler handler = new DFDescriptionHandler(mockService);
        DFRegistrationInfo info = new DFRegistrationInfo(
            "df@host", new ArrayList<>(java.util.List.of("jades://127.0.0.1:1099")),
            new ArrayList<>(), ""
        );
        when(mockService.getDFDescription()).thenReturn(info);

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockService).getDFDescription();
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).end(any(Buffer.class));
    }

    @Test
    public void Given_NonMainContainer_When_GetDescription_Then_Returns403() {
        // Arrange
        DFDescriptionHandler handler = new DFDescriptionHandler(mockService);
        when(mockService.getDFDescription())
            .thenThrow(new IllegalStateException("Not a Main Container"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(403), any(Throwable.class));
    }

    @Test
    public void Given_ServiceError_When_GetDescription_Then_Returns500() {
        // Arrange
        DFDescriptionHandler handler = new DFDescriptionHandler(mockService);
        when(mockService.getDFDescription()).thenThrow(new RuntimeException("Unexpected error"));

        // Act
        handler.handle(mockContext);

        // Assert
        verify(mockContext).fail(eq(500), any(Throwable.class));
    }
}
