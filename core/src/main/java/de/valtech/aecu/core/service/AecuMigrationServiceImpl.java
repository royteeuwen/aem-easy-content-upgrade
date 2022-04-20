package de.valtech.aecu.core.service;

import de.valtech.aecu.api.service.AecuException;
import de.valtech.aecu.api.service.AecuMigrationService;
import de.valtech.aecu.api.service.AecuService;
import de.valtech.aecu.core.serviceuser.ServiceResourceResolverService;
import de.valtech.aecu.core.util.runtime.RuntimeHelper;
import java.util.List;
import javax.jcr.Session;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link de.valtech.aecu.api.service.AecuMigrationService} impl that executes the AECU migration based on node store type
 */
@Component(
        service = AecuMigrationService.class,
        immediate = true,
        name = "AECU migration service"
)
public class AecuMigrationServiceImpl implements AecuMigrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AecuMigrationServiceImpl.class);

    @Reference
    private AecuService aecuService;
    @Reference
    private ServiceResourceResolverService resourceResolverService;

    @Activate
    public void activate() {
        ResourceResolver resourceResolver = getResourceResolver();
        Session session = resourceResolver.adaptTo(Session.class);
        boolean isCompositeNodeStore = RuntimeHelper.isCompositeNodeStore(session);
        if(isCompositeNodeStore) {
            startAecuMigration(resourceResolver);
        }
    }

    /**
     * Starts the AECU migration
     * @param resourceResolver the resource resolver to use
     */
    void startAecuMigration(ResourceResolver resourceResolver) {
        try {
            LOGGER.info("AECU migration started");
            List<String> migrationScripts = aecuService.getFiles(AecuService.AECU_APPS_PATH_PREFIX);
            if (!migrationScripts.isEmpty()) {
                migrationScripts.forEach(this::executeScript);
                LOGGER.info("AECU migration finished");
            } else {
                LOGGER.info("No AECU groovy scripts to execute");
            }
        } catch(AecuException ae) {
            LOGGER.error("Error while executing AECU migration", ae);
        }
    }

    /**
     * Executes a script cleanly (handles possible exception)
     * @param scriptPath the path to the script to execute
     */
    private void executeScript(String scriptPath) {
        try {
            aecuService.executeWithInstallHookHistory(scriptPath);
        } catch(AecuException ae) {
            LOGGER.error("Error when executing script " + scriptPath, ae);
        }
    }

    /**
     * Returns the resource resolver to be used
     * @return the resource resolver
     */
    private ResourceResolver getResourceResolver() {
        try {
            return resourceResolverService.getContentMigratorResourceResolver();
        } catch(LoginException le) {
            throw new RuntimeException("Error while logging in", le);
        }
    }

}

