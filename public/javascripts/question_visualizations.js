var appendBarChart = function(dataArray, el) {

  // // sample dataArray for format
  // dataArray = [
  //       {"choice":"A","count":100},
  //       {"choice":"B","count":200}
  //       ];

    // appending percentages, should be wrapped in a function
    var totalCount = dataArray.map(function(d){return d.count;})
                    .reduce(function(sum, el) { //sum
                      return sum + el;
                    }, 0);
    dataArray = dataArray.map(function(d){ d.percentage = d.count/totalCount *100; return d;});


    var margin = {top: 20, right: 20, bottom: 40, left: 40};
    var width = 400 - margin.left - margin.right;
    var height = 200 - margin.top - margin.bottom;

    var barColorOnHover = 'grey';
    var barColorDefault = 'black';

    var xScale = d3.scale.ordinal()
                    .domain( dataArray.map(function(d,i){return i;}) )
                    .rangeRoundBands([0, width], 0.3);

    // http://bl.ocks.org/d3noob/8952219
    var yScale = d3.scale.linear()
                    // .domain([0,100]) // 0% to 100%
                    .domain([0,Math.max.apply(null, dataArray.map(function(d){return d.percentage;}))]) // dynamic, based on max(d.percentage)
                    .range([height,0]);

    var xAxis = d3.svg.axis()
                  .scale(xScale)
                  .orient("bottom")
                  .ticks(dataArray.length)
                  .tickFormat(function(d,i){ return dataArray[i].choice; })
                  .tickSize(5,2) //has to has a style, or it wont show anything
                  ;

    var yAxis = d3.svg.axis()
                  .scale(yScale)
                  .orient("left")
                  // .tickValues([0,25,50,75,100]) //data values
                  .ticks(3) //only approximate, d3 will decide exact num of ticks
                  .tickFormat(function(d){ return d+"%"; })
                  .tickSize(-5,-2) //has to has a style, or it wont show anything
                  ;

    var canvas = d3.select(el)
                    .append("svg")
                    .attr("width", width + margin.left + margin.right)
                    .attr("height", height + margin.top + margin.bottom)
                    .append("g")
                    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
                    ;

    canvas.append("g")
          .attr("class", "xAxis")
          .attr("transform", "translate(0," + height + ")")
          .call(xAxis);

    canvas.append("g")
          .attr("class", "yAxis")
          .call(yAxis);

    // http://bl.ocks.org/biovisualize/1016860
    var tooltip = d3.select(el)
                    .append("div")
                    .attr("class", "bar-chart-tooltip")
                    .style("position", "absolute")
                    .style("z-index", "10")
                    .style("visibility", "hidden")
                    .html("") //just initializing
                    ;

    var getTooltipText = function (d) {
      var text =  "answer: " + d.choice +
                  "<br>" +
                  "percentage: " + parseInt(d.percentage) + "%" +
                  "<br>" +
                  "count: " + parseInt(d.count) +
                  "<br>" +
                  "correctness: " + "N/A"
                  ;
      return text;
    };

    var bars = canvas.append("g")
          .attr("class", "bars")
          .selectAll(".bars") //returns empty selection
          .data(dataArray)
          .enter()
            .append("rect")
            .attr("class", "bars")
            .attr('x', function(d,i) { return xScale.range()[i];} )
            .attr('y', function(d) { return yScale(d.percentage); } )
            .attr('width', xScale.rangeBand() )
            .attr('height', function(d) { return height - yScale(d.percentage); })
            .attr('fill',barColorDefault)
            .on('mouseover', function(d){
                  d3.select(this)
                  .attr('fill',barColorOnHover);
                  tooltip.html(getTooltipText(d)); // can display "wrong answer" , "correct answer" here
                  tooltip.style("visibility", "visible");
                  })
            .on('mouseout', function(d){
                  d3.select(this)
                  .attr('fill',barColorDefault);
                  tooltip.style("visibility", "hidden");
                  })
            .on("mousemove", function(){
                  tooltip.style("top", (d3.event.pageY+8)+"px").style("left",(d3.event.pageX+13)+"px");
            })
    ;
}


var record_student_answer = function() {
    // disables all student_choice elements and sends student data to the database
    var student_answers_array = [];
    $('.student_choice').each(function(ind, elem){
        $(this).prop('disabled', true);
        if ($(this).hasClass('active')) {
            student_answers_array.push(ind.toString());
        }
    })

    // only submit if at least one answer has been chosen
    if (student_answers_array.length > 0) {
        $.ajax({
            type: 'POST',
            url: window.location.pathname + "/multipleChoice/" + student_answers_array.join('_'),
            success: function(d){
              console.log("New answer recorded");
            },
            error: function(obj, st, err){
              alert(err + "\n" + obj.responseText);
            }
        })
    }
};


$(document).ready(function (){
  $('.student_choice').click(function(){
    $(this).toggleClass('active');
  });

  $('#multiple_choice_submit_button').click(function(){
    $(this).prop('disabled', true).hide();
    record_student_answer();
  });


  var dataArray;

  $('#multiple_choice button#multiple_choice_show_results').click(function(){
    var el = "div#multiple_choice_chart";

    if(typeof dataArray === 'undefined') {
        if (!($('#multiple_choice_submit_button').prop('disabled'))) {
            // if the student answer has not been recorded yet, do it now.
            $('#multiple_choice_submit_button').prop('disabled', true).hide();
            record_student_answer();
        }

        $.ajax({
            type: 'GET',
            url: window.location.pathname + "/multiple_choice_data_array",
            contentType: "application/json; charset=utf-8",
            dataType: "json",
            success: function(response){
                dataArray = response["multiple_choice_answers"];
                appendBarChart(dataArray, el);
            },
            error: function(obj, st, err){
                console.log("error");
            }
        });
    } else {
      //toggle visibility
      $(el).toggle();
    }

    var text = $('button#multiple_choice_show_results').text();
    $('button#multiple_choice_show_results').text(text == "Show results!" ? "Hide results" : "Show results!");
  });
});
