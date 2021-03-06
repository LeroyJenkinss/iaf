package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.PkiUtil;

public class SignaturePipeTest extends PipeTestBase<SignaturePipe> {

	private String testMessage = "xyz";
	private String testSignature = "JBKjNltZoFlQTsBgstpnIB4itBxzAohRXGpIWuIQh51F64P4WdT+R/55v+cHrPsQ2B49GhROeFUyy7kafOKTfMTjm7DQ5yT/srImFTlZZZbHbvQns2NWBE8DoQKt6SOYowDNIJY5qDV+82k6xY2BcTcZoiAPB53F3rEkfzz/QkxcFiCKvtg2voG1WyVkyoue10404UXIkSXv0ySYnRBRugdPO1DKyUwL6FS5tP2p8toBVzeRT6rMkEwuU3A5riQpdnEOi0ckeFvSNU3Cdgdah4HWd+48gXzBE6Uwu/BMOrD/5mRUnS0wmPn7dajkjHNC2r9+C1jxlFy3NIim1rS2iA==";


	@Override
	public SignaturePipe createPipe() {
		return new SignaturePipe();
	}

	@Test
	public void testSign() throws Exception {
		String pfxCertificate = "/Signature/certificate.pfx";
		String pfxPassword = "geheim";

		URL pfxURL = ClassUtils.getResourceURL(pfxCertificate);
		assertNotNull("PFX file not found", pfxURL);
		KeyStore keystore = PkiUtil.createKeyStore(pfxURL, pfxPassword, "pkcs12", "junittest");
		KeyManager[] keymanagers = PkiUtil.createKeyManagers(keystore, pfxPassword, null);
		if (keymanagers==null || keymanagers.length==0) {
			fail("No keymanager found in PFX file ["+pfxCertificate+"]");
		}
		X509KeyManager keyManager = (X509KeyManager)keymanagers[0];
		PrivateKey privateKey = keyManager.getPrivateKey("1");

		String alias = "1";
		String[] aliases = null;
		if(privateKey == null) {
			try {
				aliases = keyManager.getServerAliases("RSA", null);
				if(aliases != null) { // Try the first alias
					privateKey = keyManager.getPrivateKey(aliases[0]);
					assertNotNull(privateKey);
					alias = aliases[0];
				}
			} catch (Exception e) {
				System.out.println("unable to retreive alias from PFX file");
			}
		}
		assertNotNull((aliases != null) ? ("found aliases "+Arrays.asList(aliases)+" in PFX file") : "no aliases found in PFX file", privateKey);

		pipe.setKeystore("/Signature/certificate.pfx");
		pipe.setKeystorePassword(pfxPassword);
		pipe.setKeystoreAlias(alias); //GitHub Actions uses a different X509KeyManager, the first alias is 0 instead of 1;
		configureAndStartPipe();

		PipeRunResult prr = doPipe(new Message(testMessage));

		assertFalse("base64 signature should not be binary", prr.getResult().isBinary()); // Base64 is meant to be able to handle data as String. Having it as bytes causes wrong handling, e.g. as parameters to XSLT
		assertEquals(testSignature, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	public void testSignPem() throws Exception {
		pipe.setKeystore("/Signature/privateKey.key");
		pipe.setKeystoreType("pem");
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(testMessage));
		
		assertFalse("base64 signature should not be binary", prr.getResult().isBinary()); // Base64 is meant to be able to handle data as String. Having it as bytes causes wrong handling, e.g. as parameters to XSLT
		assertEquals(testSignature, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	public void testVerifyOK() throws Exception {
		pipe.setAction("verify");
		pipe.setKeystore("/Signature/certificate.pfx");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreAlias("1");
		
		Parameter param = new Parameter();
		param.setName("signature");
		param.setValue(testSignature);
		pipe.addParameter(param);
		
		PipeForward failure = new PipeForward();
		failure.setName("failure");
		pipe.registerForward(failure);
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(testMessage));

		assertEquals(testMessage, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	public void testVerifyNotOK() throws Exception {
		pipe.setAction("verify");
		pipe.setKeystore("/Signature/certificate.pfx");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreAlias("1");
		
		Parameter param = new Parameter();
		param.setName("signature");
		param.setValue(testSignature);
		pipe.addParameter(param);
		
		PipeForward failure = new PipeForward();
		failure.setName("failure");
		pipe.registerForward(failure);
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message("otherMessage"));

		assertEquals("failure", prr.getPipeForward().getName());
	}

	@Test
	public void testVerifyOKPEM() throws Exception {
		pipe.setAction("verify");
		pipe.setKeystore("/Signature/certificate.crt");
		pipe.setKeystoreType("pem");
		
		Parameter param = new Parameter();
		param.setName("signature");
		param.setValue(testSignature);
		pipe.addParameter(param);
		
		PipeForward failure = new PipeForward();
		failure.setName("failure");
		pipe.registerForward(failure);
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(testMessage));

		assertEquals(testMessage, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

}
