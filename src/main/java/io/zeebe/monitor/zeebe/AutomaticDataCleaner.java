package io.zeebe.monitor.zeebe;

import static java.time.temporal.ChronoUnit.DAYS;

import io.zeebe.monitor.entity.ProcessInstanceEntity;
import io.zeebe.monitor.repository.ElementInstanceRepository;
import io.zeebe.monitor.repository.ErrorRepository;
import io.zeebe.monitor.repository.IncidentRepository;
import io.zeebe.monitor.repository.JobRepository;
import io.zeebe.monitor.repository.MessageSubscriptionRepository;
import io.zeebe.monitor.repository.ProcessInstanceRepository;
import io.zeebe.monitor.repository.TimerRepository;
import io.zeebe.monitor.repository.VariableRepository;
import io.zeebe.monitor.rest.ExceptionHandler;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AutomaticDataCleaner {

  private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandler.class);

  private static final int NO_OF_INSTANCES_TO_DELETE = 512;

  @Autowired private ProcessInstanceRepository processInstanceRepository;
  @Autowired private ElementInstanceRepository elementInstanceRepository;
  @Autowired private VariableRepository variableRepository;
  @Autowired private JobRepository jobRepository;
  @Autowired private IncidentRepository incidentRepository;
  @Autowired private MessageSubscriptionRepository messageSubscriptionRepository;
  @Autowired private TimerRepository timerRepository;
  @Autowired private ErrorRepository errorRepository;

  @Value("${spring.data.auto-delete-data-enabled}")
  private Boolean autoDeleteDataEnabled;

  @Value("${spring.data.auto-delete-data-after-days}")
  private Integer autoDeleteDataAfterDays;

  @Scheduled(fixedDelay = 60 * 60 * 1000L)
  @Transactional
  public void autoDeleteOldInstanceData() {
    if (!autoDeleteDataEnabled) {
      return;
    }

    avoid_parallel_runs_collide_by_delay();

    LocalDateTime ts = LocalDateTime.now().minus(autoDeleteDataAfterDays, DAYS);
    ZoneOffset localZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(ts);
    long ts_microSeconds = ts.toEpochSecond(localZoneOffset) * 1000;
    Pageable p = Pageable.ofSize(NO_OF_INSTANCES_TO_DELETE);
    Page<ProcessInstanceEntity> instances =
        processInstanceRepository.findByStartLessThan(ts_microSeconds, p);

    List<Long> keys =
        instances.stream().map(ProcessInstanceEntity::getKey).collect(Collectors.toList());

    if (keys.size() > 0) {
      LOG.info("Deleting (house keeping) " + keys.size() + " process instances.");
      elementInstanceRepository.deleteByProcessInstanceKeyIn(keys);
      variableRepository.deleteByProcessInstanceKeyIn(keys);
      jobRepository.deleteByProcessInstanceKeyIn(keys);
      incidentRepository.deleteByProcessInstanceKeyIn(keys);
      messageSubscriptionRepository.deleteByProcessInstanceKeyIn(keys);
      timerRepository.deleteByProcessInstanceKeyIn(keys);
      errorRepository.deleteByProcessInstanceKeyIn(keys);
      processInstanceRepository.deleteByKeyIn(keys);
    }
  }

  private void avoid_parallel_runs_collide_by_delay() {
    try {
      Thread.sleep(RandomGenerator.getDefault().nextLong(8192, 2 * 8192));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
