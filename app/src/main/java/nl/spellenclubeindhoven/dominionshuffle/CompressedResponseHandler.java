package nl.spellenclubeindhoven.dominionshuffle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;

public class CompressedResponseHandler implements ResponseHandler<String> {
	private static final int IO_BUFFER_SIZE = 4 * 1024;

	public String handleResponse(HttpResponse response)
			throws ClientProtocolException, IOException {
		StatusLine statusLine = response.getStatusLine();
		if (statusLine.getStatusCode() >= 300) {
			throw new HttpResponseException(statusLine.getStatusCode(),
					statusLine.getReasonPhrase());
		}

		HttpEntity entity = response.getEntity();
		if (entity == null)
			return null;

		// Because of a bug in GZIPInputStream (android 1.5) we first download the
		// file, and then pass is through GZIPInputStream
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		copy(entity.getContent(), buffer);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copy(new GZIPInputStream(new ByteArrayInputStream(buffer.toByteArray())), out);
		
		return out.toString();
	}

	private static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] b = new byte[IO_BUFFER_SIZE];
		int read;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
		}
	}
}
