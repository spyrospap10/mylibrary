package com.example.mylibrary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2

class HomeFragment : Fragment() {
    private lateinit var viewPager: ViewPager2
    private lateinit var layoutDots: LinearLayout
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        viewPager = view.findViewById(R.id.viewPagerCarousel)
        layoutDots = view.findViewById(R.id.layoutDots)
        val carouselItems = listOf(
            CarouselItem(R.drawable.slide1, "Καλωσήρθατε", "Ανακαλύψτε χιλιάδες βιβλία και μοναδικές υπηρεσίες."),
            CarouselItem(R.drawable.slide2, "Νέες Αφίξεις", "Δείτε τα τελευταία βιβλία που προστέθηκαν."),
            CarouselItem(R.drawable.slide4, "Κάνε Συνδρομή", "Για απεριόριστους δανεισμούς βιβλίων."),
            CarouselItem(R.drawable.slide5, "Είσαι φοιτητής;", "Επιβεβαίωσε την φοιτητική σου ταυτότητα για δωρεάν συνδρομή 1 έτους."),
            CarouselItem(R.drawable.slide7, "Διάβασμα & Μελέτη", "Σύγχρονοι χώροι για το διάβασμά σας."),
            CarouselItem(R.drawable.slide6, "Ωράριο Λειτουργίας", "Καθημερινά:  08:00-20:00\nΣαββατοκύριακα:  10:00-18:00"),
            CarouselItem(R.drawable.slide3, "Επικοινωνήστε μαζί μας", "Ερμού 15, Αθήνα  \uD83D\uDCDE2101234567\nΤσιμισκή 40, Θεσσαλονίκη  \uD83D\uDCDE2310123456")
        )
        val adapter = CarouselAdapter(carouselItems)
        viewPager.adapter = adapter

        setupIndicators(carouselItems.size)
        setCurrentIndicator(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
            }
        })

        return view
    }

    private fun setupIndicators(count: Int) {
        val indicators = arrayOfNulls<ImageView>(count)
        val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(8, 0, 8, 0)
        for (i in indicators.indices) {
            indicators[i] = ImageView(requireContext())
            indicators[i]?.setImageDrawable(ContextCompat.getDrawable(requireContext(), android.R.drawable.presence_invisible))
            indicators[i]?.layoutParams = layoutParams
            layoutDots.addView(indicators[i])
        }
    }

    private fun setCurrentIndicator(index: Int) {
        val childCount = layoutDots.childCount
        for (i in 0 until childCount) {
            val imageView = layoutDots.getChildAt(i) as ImageView
            if (i == index) {
                imageView.alpha = 1f
                imageView.scaleX = 1.2f
                imageView.scaleY = 1.2f
                imageView.setImageResource(android.R.drawable.presence_online)
            } else {
                imageView.alpha = 0.5f
                imageView.scaleX = 1f
                imageView.scaleY = 1f
                imageView.setImageResource(android.R.drawable.presence_invisible)
            }
        }
    }
}