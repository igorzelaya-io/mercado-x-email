package hn.shadowcore.mercadoxemail.util;


import hn.shadowcore.mercadoxlibrary.entity.response.dto.EmailEventDto;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class KafkaIdempotencyAspect {

    private final RedisIdempotencyChecker idempotencyChecker;

    @Around("@annotation(KafkaIdempotent)")
    public Object checkIdempotency(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();

        if (args.length == 0 || !(args[0] instanceof EmailEventDto<?> dto)) {
            throw new IllegalArgumentException("KafkaIdempotent methods must accept EmailEventDto as first argument.");
        }

        if (idempotencyChecker.isDuplicate(dto.getEventId())) {
            return null;
        }

        return pjp.proceed();
    }

}

