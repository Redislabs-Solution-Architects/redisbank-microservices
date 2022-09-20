package com.redis.demos.redisbankpfm;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.timeseries.Sample;
import com.redis.lettucemod.timeseries.TimeRange;

import io.lettuce.core.ScoredValue;

@RestController
public class PfmController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PfmController.class);
    private final StatefulRedisModulesConnection<String, String> pfmRedis;

    public PfmController(StatefulRedisModulesConnection<String, String> redis) {
        this.pfmRedis = redis;
    }

    @GetMapping("/biggestspenders")
    public BiggestSpenders biggestSpenders(@RequestParam String iban) {

        BiggestSpenders biggestSpenders;

        try {
            List<ScoredValue<String>> values = pfmRedis.sync().zrangeWithScores("biggest_spenders_" + iban, 0,
                    Long.MAX_VALUE);
            if (values.size() > 0) {
                biggestSpenders = new BiggestSpenders(values.size());
                int i = 0;
                for (ScoredValue<String> value : values) {
                    biggestSpenders.getSeries()[i] = Math.floor(value.getScore() * 100) / 100;
                    biggestSpenders.getLabels()[i] = value.getValue();
                    i++;
                }
                return biggestSpenders;
            } else {
                biggestSpenders = new BiggestSpenders(0);
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            biggestSpenders = new BiggestSpenders(0);
        }

        return biggestSpenders;
    }

    @GetMapping("/balance")
    public Balance[] balance(@RequestParam String iban) {

        Balance[] balanceTs;

        try {
            List<Sample> tsValues = pfmRedis.sync().tsRange("balance_over_time_" + iban,
                    TimeRange.from(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 7))
                            .to(System.currentTimeMillis()).build());
            balanceTs = new Balance[tsValues.size()];
            int i = 0;

            for (Sample entry : tsValues) {
                Object keyString = entry.getTimestamp();
                Object valueString = entry.getValue();
                balanceTs[i] = new Balance(keyString, valueString);
                i++;
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            balanceTs = new Balance[0];
        }

        return balanceTs;
    }

}
