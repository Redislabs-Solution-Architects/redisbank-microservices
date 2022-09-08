package com.redis.demos.redisbankpfm;

import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.timeseries.Sample;
import com.redis.lettucemod.timeseries.TimeRange;

@RestController
public class PfmController {

    private final StatefulRedisModulesConnection<String, String> redis;
    private final StringRedisTemplate redisTemplate;

    public PfmController(StatefulRedisModulesConnection<String, String> redis, StringRedisTemplate redisTemplate) {
        this.redis = redis;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/biggestspenders")
    public BiggestSpenders biggestSpenders(@RequestParam String iban) {
        Set<TypedTuple<String>> range = redisTemplate.opsForZSet()
                .rangeByScoreWithScores("biggest_spenders_" + iban, 0, Double.MAX_VALUE);
        if (range.size() > 0) {
            BiggestSpenders biggestSpenders = new BiggestSpenders(range.size());
            int i = 0;
            for (TypedTuple<String> typedTuple : range) {
                biggestSpenders.getSeries()[i] = Math.floor(typedTuple.getScore() * 100) / 100;
                biggestSpenders.getLabels()[i] = typedTuple.getValue();
                i++;
            }
            return biggestSpenders;
        } else {
            return new BiggestSpenders(0);
        }

    }

    @GetMapping("/balance")
    public Balance[] balance(@RequestParam String iban) {
        List<Sample> tsValues = redis.sync().tsRange("balance_over_time_" + iban,
                TimeRange.from(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 7))
                        .to(System.currentTimeMillis()).build());
        Balance[] balanceTs = new Balance[tsValues.size()];
        int i = 0;

        for (Sample entry : tsValues) {
            Object keyString = entry.getTimestamp();
            Object valueString = entry.getValue();
            balanceTs[i] = new Balance(keyString, valueString);
            i++;
        }

        return balanceTs;
    }

}
