package org.apereo.cas.config;

import org.apereo.cas.aws.ChainingAWSCredentialsProvider;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.support.saml.OpenSamlConfigBean;
import org.apereo.cas.support.saml.metadata.resolver.AmazonS3SamlRegisteredServiceMetadataResolver;
import org.apereo.cas.support.saml.services.idp.metadata.cache.resolver.SamlRegisteredServiceMetadataResolver;
import org.apereo.cas.support.saml.services.idp.metadata.plan.SamlRegisteredServiceMetadataResolutionPlan;
import org.apereo.cas.support.saml.services.idp.metadata.plan.SamlRegisteredServiceMetadataResolutionPlanConfigurator;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is {@link SamlIdPAmazonS3RegisteredServiceMetadataConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Configuration("samlIdPAmazonS3RegisteredServiceMetadataConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class SamlIdPAmazonS3RegisteredServiceMetadataConfiguration implements SamlRegisteredServiceMetadataResolutionPlanConfigurator {

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier("shibboleth.OpenSAMLConfig")
    private OpenSamlConfigBean openSamlConfigBean;

    @Bean
    public SamlRegisteredServiceMetadataResolver amazonS3SamlRegisteredServiceMetadataResolver() {
        val idp = casProperties.getAuthn().getSamlIdp();
        return new AmazonS3SamlRegisteredServiceMetadataResolver(idp, openSamlConfigBean, amazonS3Client());
    }

    @ConditionalOnMissingBean(name = "amazonS3Client")
    @Bean
    @RefreshScope
    public AmazonS3 amazonS3Client() {
        val amz = casProperties.getAuthn().getSamlIdp().getMetadata().getAmazonS3();
        val endpoint = new AwsClientBuilder.EndpointConfiguration(amz.getEndpoint(), amz.getRegion());
        val credentials = ChainingAWSCredentialsProvider.getInstance(amz.getCredentialAccessKey(),
            amz.getCredentialSecretKey(),
            amz.getCredentialsPropertiesFile(),
            amz.getProfilePath(),
            amz.getProfileName());
        return AmazonS3ClientBuilder
            .standard()
            .withCredentials(credentials)
            .withRegion(amz.getRegion())
            .withEndpointConfiguration(endpoint)
            .build();
    }

    @Override
    public void configureMetadataResolutionPlan(final SamlRegisteredServiceMetadataResolutionPlan plan) {
        plan.registerMetadataResolver(amazonS3SamlRegisteredServiceMetadataResolver());
    }
}
