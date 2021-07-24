package module;

import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.setting.Setting;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;


public abstract class Module {
    private static final Log log = LogFactory.get();

    private Setting setting;
    private CacheManager cacheManager;

    protected Setting setting() {
        if (setting == null) {
            setting = new Setting("config.setting");
            log.info("创建setting配置:{}", setting);
        }
        return setting;
    }

    protected Cache cache(String cacheName) {
        if (cacheManager == null) {
            cacheManager = CacheManager.create(new ClassPathResource("ehcache.xml").getUrl());
            log.info("创建cacheManager:{}", cacheManager);
        }
        return cacheManager.getCache(cacheName);
    }

    public abstract void start();


}
