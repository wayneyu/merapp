 $(document).ready(function (){

    $("div.hideable_box>div+div").fadeOut(0);

    // click the title
    // div.hideable_box>div
    // hide the box below it
    // div.hideable_box>div+div
    $(".hideable_box>.box_title").click(function (){
      $(this).siblings().fadeToggle(100);
     });

    $(".latex_edit_render_area").fadeOut(0);

    $("[id$='_edit'] .render_button").click(function (){
        var parentId = $(this).parent().parent().parent().attr('id');
        $(this).text($(this).text() == "Edit" ? "Render" : "Edit");
        var textarea = $("#" + parentId + " textarea");
        var renderarea = $("#" + parentId + " .latex_edit_render_area");
        textarea.fadeToggle(10);
        renderarea.fadeToggle(10);
        var latex_code = textarea.val();
        renderarea.empty();
        renderarea.append(latex_code);
        MathJax.Hub.Queue(["Typeset",MathJax.Hub]); // refreshes the view in some sense
    });

    $("[id$='_edit'] .latex_edit_render_area").click(function() {
        var parentId = $(this).parent().parent().attr('id');
        var textarea = $("#" + parentId + " textarea");
        var renderarea = $("#" + parentId + " .latex_edit_render_area");
        var button = $("#" + parentId + " .render_button");
        button.text(button.text() == "Edit" ? "Render" : "Edit");
        textarea.fadeToggle(10);
        renderarea.fadeToggle(10);
    });

    $("[id$='_edit'] .submit_button").click(function (){
        var url = window.location.pathname;

        var parentId = $(this).parent().parent().parent().attr('id');
        var newValue = $("#" + parentId + " textarea").val();

//        key is of the form type_html_edit or type_html-num_edit where type is one of [statement, hints, answer, sols]
//        if type indicates the array hints or sols we want to rewrite it to type_html.num because that is what MongoDB expects
        var data = {};
        var key = parentId.split('_edit')[0].replace('-', '.');
        data[key] = newValue;

        $.ajax({
          contentType: 'application/json',
          type: 'POST',
          url: url,
          data: JSON.stringify(data),
          success: function(d){
            location.reload();
          },
          error: function(obj, st, err){
            alert(err);
          }
        });
    });



    //$(".latex_box #tags ul li:nth-child(1)").click(function() {   //this will apply to all anchor tags
    $(".latex_box").find("#button1").click(function() {   //this will apply to all anchor tags
       //$(".latex_box textarea").val($(".latex_box textarea").val()+'$x_1$');
       $(this).closest(".latex_box").find("textarea").insertAtCaret('_{n}',false);
    });

    //$(".latex_box #tags ul li:nth-child(2)").click(function() {   //this will apply to all anchor tags
    $(".latex_box").find("#button2").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\int_{a}^{b} f(x)\\,dx',false);
    });

    $(".latex_box").find("#button3").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\alpha',false);
    });

    $(".latex_box").find("#button4").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\frac{d}{dx}',false);
    });

    $(".latex_box").find("#button5").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('<font color="blue"> blue text </font>',true)
    });

    $(".latex_box").find("#button6").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\n\\begin{equation}\n f(x) = g(x) + a \n\\end{equation} \n',true)
    });

    $(".latex_box").find("#button7").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\left(   \\right)',false)
    });

    $(".latex_box").find("#button8").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\left[   \\right]',false)
    });

    $(".latex_box").find("#button9").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\lim_{x \\rightarrow a}',false)
    });

    $(".latex_box").find("#button10").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\infty',false)
    });

    $(".latex_box").find("#button11").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\pi',false)
    });

    $(".latex_box").find("#button12").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\beta',false)
    });

    $(".latex_box").find("#button13").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\gamma',false)
    });

    $(".latex_box").find("#button14").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\delta',false)
    });

    $(".latex_box").find("#button15").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\epsilon',false)
    });

    $(".latex_box").find("#button16").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\n\\begin{align}\n f(x) &= g(x) + a \\\\ \n      &= \\cos(x) \n\\end{align} \n',true)
    });

    $(".latex_box").find("#button17").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('^{p}',false)
    });

    $(".latex_box").find("#button18").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\lambda',false)
    });

    $(".latex_box").find("#button19").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\frac{d^{2}}{dx^{2}}',false);
    });

    $(".latex_box").find("#button20").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\frac{a}{b}',false);
    });

    $(".latex_box").find("#button21").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\mathbb{R}',false);
    });

    $(".latex_box").find("#button22").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\mathbb{C}',false);
    });

    $(".latex_box").find("#button23").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\leq',false);
    });

    $(".latex_box").find("#button24").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\geq',false);
    });

    $(".latex_box").find("#button25").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('>',false);
    });

    $(".latex_box").find("#button26").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('<',false);
    });

    $(".latex_box").find("#button27").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\sin(  )',false);
    });

    $(".latex_box").find("#button28").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\cos(  )',false);
    });

    $(".latex_box").find("#button29").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\sim',false);
    });

    $(".latex_box").find("#button30").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\sum_{i = 0}^{N}',false);
    });

    $(".latex_box").find("#button31").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\dots',false);
    });

    $(".latex_box").find("#button32").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('<font color="red"> red text </font>',true)
    });

    $(".latex_box").find("#button33").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('\\begin{bmatrix} a & b \\\\ c & d \\end{bmatrix}',false);
    });

    $(".latex_box").find("#button34").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find("textarea").insertAtCaret('f(x) = \\begin{cases} a &\\mbox{if } x \\geq 0 \\\\ b &\\mbox{if } x < 0 \\end{cases}',false);
    });

    $(".latex_box button").click(function() {   //this will apply to all anchor tags
       $(this).closest(".latex_box").find(".latex_edit_render_area").empty();
       $(this).closest(".latex_box").find(".latex_edit_render_area").append( $(this).closest(".latex_box").find("textarea").val() );
       MathJax.Hub.Queue(["Typeset", MathJax.Hub]); // refreshes the view in some sense
    });



    $("#courseSelect").on('change', function(e){
        var course = $("#courseSelect option:selected").text();
        $.ajax({
          type: 'GET',
          url: "/questions/search/term/" + course,
          //TODO: replace URL with play's javascriptRoutes
          success: function(d){
              var term_selector = $("#termSelect")
              term_selector.empty()
              $.each(d, function(value, term) {
                var option = $('<option />').val(term).text(term);
                term_selector.append(option)
              })
              $("#termSelect").change();
           }
        })
    })

    $("#termSelect").on('change', function(e){
        var course = $("#courseSelect option:selected").text();
        var term = $("#termSelect option:selected").text();
        $.ajax({
          type: 'GET',
          url: "/questions/search/year/" + course + "/" + term,
          //TODO: replace URL with play's javascriptRoutes
          success: function(d){
            var year_selector = $("#yearSelect")
            year_selector.empty()
            $.each(d, function(value, year) {
                var option = $('<option />').val(year).text(year);
                year_selector.append(option)
            })
            $("#yearSelect").change();
          }
        })
    });

    $("#yearSelect").on('change', function(e){
        var course = $("#courseSelect option:selected").text();
        var term = $("#termSelect option:selected").text();
        var year = $("#yearSelect option:selected").text();
        $.ajax({
          type: 'GET',
          url: "/questions/search/question/" + course + "/" + term + "_" + year,
          //TODO: replace URL with play's javascriptRoutes
          success: function(d){
            var question_selector = $("#questionSelect")
            question_selector.empty()
            $.each(d, function(index, q) {
                var option = $('<option />').val(q.number).text(q.number_human);
                question_selector.append(option)
            })
          }
        })
    });

    $("#questionSelect").on('change', function(e){
        $("#findQuestionBtn").click();
    });

    var current_question = $("#questionSelect option:selected").val();
    var all_questions = [];
    $("#questionSelect").children().each(function() {all_questions.push($(this).val())});
    var current_question_index = all_questions.indexOf(current_question);
    if (current_question_index == 0) {
            $("#prev_question").remove();
        } else {
            $("#prev_question").prop("href", all_questions[current_question_index-1]);
        }
    if (current_question_index == all_questions.length - 1) {
            $("#next_question").remove();
        } else {
            $("#next_question").prop("href", all_questions[current_question_index+1]);
        }

    $(".easiness_rating").each(function(){
        var val = parseFloat($(this).text());
        if (val < 1.5) {var col = "#d9534f";}
        else if (val < 2.5) {var col = "#f0ad4e";}
        else if (val < 3.5) {var col = "#5bc0de";}
        else if (val < 4.5) {var col = "#5cb85c";}
        else if (val <= 5) {var col = "#008000";}
        else {var col = "#A0A0A0";} // no vote available
        $(this).css("color", col);
        $(this).attr("title", $(this).attr("title") + " vote(s)");
        });


});

$.fn.insertAtCaret = function (insertion,notMath) {
  return this.each(function(){
    if (document.selection) {
      //IE support
      this.focus();
      sel = document.selection.createRange();
      sel.text = insertion;
      this.focus();
    }else if (this.selectionStart || this.selectionStart == '0') {
      //MOZILLA/NETSCAPE support
      startPos = this.selectionStart;
      endPos = this.selectionEnd;
      scrollTop = this.scrollTop;
      if(notMath){
        this.value = this.value.substring(0, startPos) + insertion + this.value.substring(endPos,this.value.length);
        this.focus();
        this.selectionStart = startPos + insertion.length;
        this.selectionEnd = startPos + insertion.length;
      } 
      else {
      if (isInMathEnvironment(this.value.substring(0, startPos))) {
        this.value = this.value.substring(0, startPos) + insertion + ' ' + this.value.substring(endPos,this.value.length);
        this.focus();
        this.selectionStart = startPos + insertion.length + 1;
        this.selectionEnd = startPos + insertion.length + 1;
      }
      else {
        this.value = this.value.substring(0, startPos) + '$' + insertion + '$' + this.value.substring(endPos,this.value.length);
        this.focus();
        this.selectionStart = startPos + insertion.length + 2;
        this.selectionEnd = startPos + insertion.length + 2;
      }
    }
      this.scrollTop = scrollTop;
    } else {
      this.value += insertion;
      this.focus();
    }
  });
};

function isInMathEnvironment(aString) {
  //Check if the string contains an odd number of '$'s
  var n = aString.split('$').length - 1;
  var bool = (n%2 == 1);

  //Check if the cursor is in the {equation} env.
  var nb = aString.split('\\begin{equation}').length - 1;
  var ne = aString.split('\\end{equation}').length - 1;
  bool = bool | (nb - ne == 1);

  //Check if the cursor is in the {align} env.
  var nb = aString.split('\\begin{align}').length - 1;
  var ne = aString.split('\\end{align}').length - 1;
  bool = bool | (nb - ne == 1);

  return bool;

};



$(document).ready(function(){
$("#my_rating").rating({
    stars: 5,
    min: 0,
    max: 5,
    step: 1,
    starCaptions: {1: "Way over my head", 2: "Difficult", 3: "A good challenge", 4: "Relatively simple",
                   5: "Easy as &pi;"},
    starCaptionClasses: {1: "text-danger",
                         2: "text-warning",
                         3: "text-info",
                         4: "text-primary",
                         5: "text-success"}
});

$('#my_rating').on('rating.change', function(event, value, caption) {
    debugger;
    console.log(value);
    console.log(caption);
    $.ajax({
      type: 'POST',
      url: window.location.pathname + "/vote/" + value,
      success: function(d){
        debugger;
        $("span.easiness_rating")[0].textContent = d.rating.toFixed(1);
        $("span.num_votes")[0].textContent = "(" + d.num_votes + " votes)";
      },
      error: function(obj, st, err){
        alert(err);
      }
    })
});

var topicsCache = [];
$(".question_tags").select2({
    tags: true,
    tokenSeparators: [",", " "],
    minimumInputLength: 1,
    createSearchChoice: function(term, data) {
        if ($(data).filter(function() {
          return this.text.localeCompare(term) === 0;
        }).length === 0) {
          return {
            id: term,
            text: term
          };
        }
    },
    multiple: true,
    data:{ results: topicsCache},
    initSelection: function (element, callback) {
        var data = [];
        $(element.val().split(",")).each(function () {
            data.push({id: this, text: this});
        });
        callback(data);
    }
})
.on("select2-selecting", function(e){
    var newTag = e.choice.text;
    console.log(newTag);
    $.ajax({
        type: 'POST',
        url: window.location.pathname + "/addtopic/" + newTag,
        success: function(d){
            debugger;
            $(".question_tags");
            console.log("added new tag");
        },
        error: function(obj, st, err){
            alert(err + "\n" + obj.responseText);
        }
    })
})
.on("select2-removing",function(e){
    var removedTag = e.choice.text;
    console.log("removing tag " + removedTag);
    $.ajax({
        type: 'POST',
        url: window.location.pathname + "/removetopic/" + removedTag,
        success: function(d){
            console.log("removed tag");
        },
        error: function(obj, st, err){
            alert(err + "\n" + obj.responseText);
        }
    })
});
});

$('body').on('click', '.quality_buttons button', function (e) {
    if (!$(this).hasClass('active')){
        <!--Only fire if active button changed-->
        $(this).addClass('active');
        $(this).siblings().removeClass('active');
        var newQualityFlag = $(this).val();
        $.ajax({
            type: 'POST',
            url: window.location.pathname + "/updateQuality/" + newQualityFlag,
            success: function(d){
                //
            },
            error: function(obj, st, err){
                alert(err + "\n" + obj.responseText);
            }
        })
    };
});

display_prediction_result = function(reply) {
    var res = "<Strong>Most likely topics of this question:</Strong><br><ul>";
    jQuery.each(reply.topics, function() {
        res += "<li>" + this.topic.replace(/_/g, " ") + " <em>(" + this.parent_topic.replace(/_/g, " ") + ")</em></li>";
    })
    res = res.concat("</ul>");
    $("#predict_topic_result").hide().append(res).fadeIn(1200);
}

$('body').on('click', '#predict_topic_button', function (e) {
    var data = {statement: $(this).val()};
    $.ajax({
        url: "http://54.67.49.244/api/result",
        type: "POST",
        data: JSON.stringify(data, null, '\t'),
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (result) {
            display_prediction_result(result)
            $('#predict_topic_button').prop('disabled', true)
        },
        error: function (xhr, ajaxOptions, thrownError) {
             console.log('Could not predict topic');
        }
        });

})

$(".show_edit").click(function() {
  $(this).prev().toggle();
});

