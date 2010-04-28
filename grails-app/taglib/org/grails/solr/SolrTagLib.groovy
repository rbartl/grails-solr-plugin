package org.grails.solr
import org.grails.solr.SolrUtil


class SolrTagLib {
    
    static namespace = 'solr'
    
    def facet = { attrs, body ->

      def field = attrs['field']
      def result = attrs['result']
      def fq = attrs['fq']  
      def q = attrs['q']
      def action = attrs['action']
      def cssClass = (attrs['class']) ? attrs['class'] : "solr-facet"      
      def min = (attrs['min']) ? attrs['min'] as int : 1     

      def facetValues = result.queryResponse.getFacetField(field).values
      def currentFacetSelection = null

      fq.each { item ->
        if(item.contains(field)) {
          // TODO: replace hardcoded style
          currentFacetSelection = link(action:action, params:[q:q, fq: (fq - [item] )]) { "<span style='color:red; font-size:14px'>X</span>" }
          currentFacetSelection += " " + item.split(":")[1].replace("\\", "")  + " <br/>"
        }
      }      

      if((facetValues && facetValues.size() >= min) || currentFacetSelection){

        out << "<div class=\"${cssClass}\">"
        request.setAttribute("solrresult",result)
        request.setAttribute("solrfield",field)
        request.setAttribute("solrfq",fq)
        request.setAttribute("solrq",q)
        request.setAttribute("solraction",action)
        out << body()
        out << currentFacetSelection

      }
    }


    // Call the grails link taglib if this is an indexed domain, otherwise return a link from
    // a field specified in which should be the SolrDocuement attribute to use if not a domain
    // <solr:resultLink result="${result}" field="solrDocuemntFieldNameIfNotDomain"
    //
    def resultLink = { attrs, body ->
  
      def result = attrs['result']
      def altField = attrs['altField']
      def classType = result.solrDocument.getFieldValue(SolrUtil.TYPE_FIELD)
      log.trace "classType for doc is ${classType}"

      if(classType) {
        // need to get the controller value, assuming controller is same name as domain
        // not sure how to handle the packages
        // so for now, just going to strip any package and lowercase first letter brute force like
        classType.split(".")
        def i = classType.lastIndexOf(".")
        def ctrlr = (i > 0) ? classType.substring(i+1) : classType
        ctrlr = ctrlr[0].toLowerCase() + ctrlr.substring(1)
        out << link(action:"show", controller:ctrlr, id:result.id) { body() }
      } 
      else {
        def altFieldVal = result.solrDocument.getFieldValue(altField)
        if(altFieldVal)
          out << "<a href=\"${altFieldVal}\">${body()}</a> "
      }
    }

  /**
   * Nested Tag for <solr:facet />, it provides a possibility to format the Tag of the various Facet Values.
   * Example:
   *   <solr:facetvalue>
   *    <g:message code="${it.name}" default="--${it.name}--"/> (${it.count})
   *   </solr:facetvalue>
   */
  def facetvalue = { attrs, body ->

    def result = request.getAttribute("solrresult")
    def field = request.getAttribute("solrfield")
    def fq = request.getAttribute("solrfq")
    def q = request.getAttribute("solrq")
    def action = request.getAttribute("solraction")
    
    result.queryResponse.getFacetField(field).values.each { item ->

      def linkParams = [:]
      if(action)
        linkParams.action = action
      linkParams.params = [:]
      linkParams.params.q = q
      linkParams.params.fq = fq.size() ? ([item.asFilterQuery] + fq) : [item.asFilterQuery]

      out << "<ul>"
      if(!fq.contains(item.asFilterQuery) && item.count > 0) {
        out << "<li>"
        out << link(linkParams) {
          body(item)
        }
        out << "</li>"
      }
      out << "</ul>"
    }
    out << "</div>"
  }
    
}