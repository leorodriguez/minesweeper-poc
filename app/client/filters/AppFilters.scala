package client.filters

import play.api.http.DefaultHttpFilters
import play.filters.csrf.CSRFFilter
import javax.inject.Inject

class AppFilters @Inject() (csrfFilter: CSRFFilter)
  extends DefaultHttpFilters(csrfFilter)
