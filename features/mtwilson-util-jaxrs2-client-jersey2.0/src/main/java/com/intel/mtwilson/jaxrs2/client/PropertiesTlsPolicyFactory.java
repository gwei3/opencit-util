/*
 * Copyright (C) 2014 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.jaxrs2.client;

import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.dcsg.cpg.configuration.PropertiesConfiguration;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.dcsg.cpg.x509.repository.KeystoreCertificateRepository;
import com.intel.mtwilson.tls.policy.TlsPolicyDescriptor;
import com.intel.mtwilson.tls.policy.factory.TlsPolicyFactoryUtil;
//import com.intel.mtwilson.tls.policy.factory.TlsPolicyFactory;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author jbuhacoff
 */
public class PropertiesTlsPolicyFactory {

    private static Collection<X509Certificate> getCertificates(String keystoreFile, String keystorePassword) {
        try {
            KeystoreCertificateRepository repository = new KeystoreCertificateRepository(keystoreFile, keystorePassword);
            return repository.getCertificates();
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new IllegalArgumentException("Cannot load certificates", e);
        }
    }

    private static Collection<String> encodeCertificates(Collection<X509Certificate> certificates) {
        try {
            HashSet<String> set = new HashSet<>();
            for (X509Certificate certificate : certificates) {
                set.add(Base64.encodeBase64String(certificate.getEncoded()));
            }
            return set;
        } catch (CertificateEncodingException e) {
            throw new IllegalArgumentException("Cannot encode certificates", e);
        }
    }

    public static TlsPolicy createTlsPolicy(Configuration configuration) {
        // TODO: load tls policy using configuration... per-host policy, digest, insecure, shared policy from database...
        String certificateKeystoreFile = configuration.get("mtwilson.api.tls.policy.certificate.keystore.file", null);
        String certificateKeystorePassword = configuration.get("mtwilson.api.tls.policy.certificate.keystore.password", null);
        String certificateDigestSha256 = configuration.get("mtwilson.api.tls.policy.certificate.sha256", null);
        String certificateDigestSha1 = configuration.get("mtwilson.api.tls.policy.certificate.sha1", null);
        String publicKeyKeystoreFile = configuration.get("mtwilson.api.tls.policy.publickey.keystore.file", null);
        String publicKeyKeystorePassword = configuration.get("mtwilson.api.tls.policy.publickey.keystore.password", null);
        String publicKeyDigestSha256 = configuration.get("mtwilson.api.tls.policy.publickey.sha256", null);
        String publicKeyDigestSha1 = configuration.get("mtwilson.api.tls.policy.publickey.sha1", null);
        String insecure = configuration.get("mtwilson.api.tls.policy.insecure", null);
        TlsPolicyDescriptor tlsPolicyDescriptor = new TlsPolicyDescriptor();
        tlsPolicyDescriptor.setMeta(new HashMap<String, String>());
        tlsPolicyDescriptor.setData(new ArrayList<String>());
        if (certificateKeystoreFile != null && certificateKeystorePassword != null) {
            tlsPolicyDescriptor.setPolicyType("certificate");
            tlsPolicyDescriptor.getData().addAll(encodeCertificates(getCertificates(certificateKeystoreFile, certificateKeystorePassword)));
        } else if (publicKeyKeystoreFile != null && publicKeyKeystorePassword != null) {
            tlsPolicyDescriptor.setPolicyType("public-key");
            tlsPolicyDescriptor.getData().addAll(encodeCertificates(getCertificates(certificateKeystoreFile, certificateKeystorePassword)));
        } else if (certificateDigestSha256 != null) {
            tlsPolicyDescriptor.setPolicyType("certificate-digest");
            tlsPolicyDescriptor.getMeta().put("digestAlgorithm", "SHA-256");
            tlsPolicyDescriptor.getData().add(certificateDigestSha256);
        } else if (certificateDigestSha1 != null) {
            tlsPolicyDescriptor.setPolicyType("certificate-digest");
            tlsPolicyDescriptor.getMeta().put("digestAlgorithm", "SHA-1");
            tlsPolicyDescriptor.getData().add(certificateDigestSha1);
        } else if (publicKeyDigestSha256 != null) {
            tlsPolicyDescriptor.setPolicyType("public-key-digest");
            tlsPolicyDescriptor.getMeta().put("digestAlgorithm", "SHA-256");
            tlsPolicyDescriptor.getData().add(publicKeyDigestSha256);
        } else if (publicKeyDigestSha1 != null) {
            tlsPolicyDescriptor.setPolicyType("public-key-digest");
            tlsPolicyDescriptor.getMeta().put("digestAlgorithm", "SHA-1");
            tlsPolicyDescriptor.getData().add(publicKeyDigestSha1);
        } else if (insecure != null && insecure.equalsIgnoreCase("true")) {
            tlsPolicyDescriptor.setPolicyType("INSECURE");
        }
//        TlsPolicy tlsPolicy = TlsPolicyFactory.createTlsPolicy(tlsPolicyDescriptor);
        TlsPolicy tlsPolicy = TlsPolicyFactoryUtil.createTlsPolicy(tlsPolicyDescriptor);
        return tlsPolicy;

    }

    public static TlsPolicy createTlsPolicy(Properties properties) {
        return createTlsPolicy(new PropertiesConfiguration(properties));
    }
}
