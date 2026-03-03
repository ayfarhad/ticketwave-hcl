package com.ticketwave.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

class RateLimitingFilterTest {
    private RateLimitingFilter rateLimitingFilter;
    
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private PrintWriter writer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rateLimitingFilter = new RateLimitingFilter();
    }

    @Test
    void allowRequestUnderLimit() throws ServletException, IOException {
        // arrange
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // act - make requests under the limit (100 per hour)
        for (int i = 0; i < 50; i++) {
            rateLimitingFilter.doFilter(request, response, chain);
        }

        // assert - all should be allowed
        verify(chain, times(50)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void rejectRequestOverLimit() throws ServletException, IOException {
        // arrange
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getWriter()).thenReturn(writer);

        // act - exceed limit
        for (int i = 0; i < 101; i++) {
            rateLimitingFilter.doFilter(request, response, chain);
        }

        // assert - the 101st request should be rejected
        verify(response).setStatus(429);
    }

    @Test
    void differentIpsTrackedSeparately() throws ServletException, IOException {
        // arrange
        HttpServletRequest request2 = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request2.getRemoteAddr()).thenReturn("127.0.0.2");

        // act
        for (int i = 0; i < 50; i++) {
            rateLimitingFilter.doFilter(request, response, chain);
            rateLimitingFilter.doFilter(request2, response, chain);
        }

        // assert - both should be allowed
        verify(chain, times(100)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any());
    }
}

