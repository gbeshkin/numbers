import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.picture.PaintByNumbersServlet;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.*;
import java.io.*;

import static org.mockito.Mockito.*;

class PaintByNumbersServletTest {

    private PaintByNumbersServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private Part filePart;

    private ByteArrayOutputStream responseOutputStream;

    @BeforeEach
    void setUp() throws IOException {
        servlet = new PaintByNumbersServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        filePart = mock(Part.class);

        // Mock the response output stream
        responseOutputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }

            @Override
            public void write(int b) throws IOException {
                responseOutputStream.write(b);
            }
        });
    }

    @Test
    void testDoGet() throws IOException, ServletException {
        when(request.getSession()).thenReturn(session);

        servlet.doGet(request, response);

        // Verify content type
        verify(response).setContentType("text/html");

        // Verify HTML output
        String responseOutput = responseOutputStream.toString();
        assert responseOutput.contains("<title>Painting by numbers</title>");
    }

    @Test
    void testDoPost_WithValidFile() throws IOException, ServletException {
        // Mock file upload
        when(request.getPart("file")).thenReturn(filePart);
        when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{ /* Add image bytes */ }));
        when(request.getParameter("n_colors")).thenReturn("8");

        servlet.doPost(request, response);

        // Verify the response content type
        verify(response).setContentType("image/png");

        // Verify that the response contains processed image data
        assert responseOutputStream.size() > 0;
    }

    @Test
    void testDoPost_WithInvalidFile() throws IOException, ServletException {
        // Mock invalid file upload
        when(request.getPart("file")).thenReturn(null);

        servlet.doPost(request, response);

        // Verify error response
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "No file uploaded or file is empty.");
    }
}